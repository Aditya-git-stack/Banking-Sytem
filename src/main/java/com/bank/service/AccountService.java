package com.bank.service;

import com.bank.dao.AccountDao;
import com.bank.dao.TransactionDao;
import com.bank.dao.UserDao;
import com.bank.dto.Receipt;
import com.bank.dto.Requests;
import com.bank.exception.BankException;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.User;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core banking: balance, deposit, withdraw, fund transfer, statement, debit card, security settings.
 * Also has small shared helpers (credit/debit) that the FD, Loan and Bill services reuse.
 *
 * Every money movement follows the same JDBC pattern:
 *   1. con.setAutoCommit(false)  -> start transaction
 *   2. lock the account row (SELECT ... FOR UPDATE)
 *   3. update balance + insert transaction row
 *   4. con.commit()  (or con.rollback() if anything fails)
 */
@Service
public class AccountService {

    public static final BigDecimal MAX_PER_TRANSACTION = new BigDecimal("200000");
    public static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("500000");

    private final DataSource dataSource;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final UserDao userDao;
    private final AuthService authService;

    public AccountService(DataSource dataSource, AccountDao accountDao, TransactionDao transactionDao,
                          UserDao userDao, AuthService authService) {
        this.dataSource = dataSource;
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.userDao = userDao;
        this.authService = authService;
    }

    // ------------------------------------------------------------------ read

    public Account getAccountOfUser(long userId) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            Account account = accountDao.findByUserId(con, userId);
            if (account == null) throw new BankException("No bank account found");
            return account;
        }
    }

    /** User details + account details for the dashboard. */
    public Map<String, Object> getProfile(long userId) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            User user = userDao.findById(con, userId);
            Account account = accountDao.findByUserId(con, userId);
            return account == null ? Map.of("user", user) : Map.of("user", user, "account", account);
        }
    }

    public List<Transaction> miniStatement(long userId, int limit) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            Account account = accountDao.findByUserId(con, userId);
            return transactionDao.findRecent(con, account.getId(), limit);
        }
    }

    public List<Transaction> statement(long userId, LocalDate from, LocalDate to) throws SQLException {
        if (from.isAfter(to)) throw new BankException("'From' date must be before 'To' date");
        try (Connection con = dataSource.getConnection()) {
            Account account = accountDao.findByUserId(con, userId);
            return transactionDao.findBetween(con, account.getId(), from, to);
        }
    }

    /** Shows the payee's name before sending money (like YONO does). */
    public Map<String, String> lookupRecipient(String to) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            Account account = findRecipient(con, to);
            if (account == null) throw new BankException("No account found for " + to);
            User owner = userDao.findById(con, account.getUserId());
            return Map.of("name", owner.getFullName(), "accountNumber", account.getAccountNumber(), "upiId", account.getUpiId());
        }
    }

    // ------------------------------------------------------------------ money

    public Receipt deposit(long userId, Requests.Amount r) throws SQLException {
        BigDecimal amount = checkAmount(r.amount());
        try (Connection con = dataSource.getConnection()) {
            verifyMpin(con, userId, r.mpin());
            con.setAutoCommit(false);
            try {
                Account account = lockActiveAccount(con, userId);
                Transaction t = credit(con, account, amount, "DEPOSIT", "Cash deposit", newReference());
                con.commit();
                return new Receipt(t.getReferenceNo(), amount, account.getBalance(), "Deposit successful");
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    public Receipt withdraw(long userId, Requests.Amount r) throws SQLException {
        BigDecimal amount = checkAmount(r.amount());
        try (Connection con = dataSource.getConnection()) {
            verifyMpin(con, userId, r.mpin());
            con.setAutoCommit(false);
            try {
                Account account = lockActiveAccount(con, userId);
                Transaction t = debit(con, account, amount, "WITHDRAW", "Cash withdrawal", newReference());
                con.commit();
                return new Receipt(t.getReferenceNo(), amount, account.getBalance(), "Withdrawal successful");
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Send money to another account in this bank, by account number or UPI id. */
    public Receipt transfer(long userId, Requests.Transfer r) throws SQLException {
        BigDecimal amount = checkAmount(r.amount());
        try (Connection con = dataSource.getConnection()) {
            verifyMpin(con, userId, r.mpin());
            con.setAutoCommit(false);
            try {
                Account sender = accountDao.findByUserId(con, userId);
                Account receiver = findRecipient(con, r.to().trim());
                if (receiver == null) throw new BankException("Recipient account / UPI ID not found");
                if (receiver.getId().equals(sender.getId())) throw new BankException("You cannot send money to your own account");

                // Lock both rows. Always lock the smaller id first so two opposite
                // transfers (A->B and B->A) can never wait for each other forever (deadlock).
                Account first = accountDao.findByIdForUpdate(con, Math.min(sender.getId(), receiver.getId()));
                Account second = accountDao.findByIdForUpdate(con, Math.max(sender.getId(), receiver.getId()));
                sender = first.getId().equals(sender.getId()) ? first : second;
                receiver = (sender == first) ? second : first;

                requireActive(sender);
                if (!"ACTIVE".equals(receiver.getStatus())) throw new BankException("Recipient account is not active");
                if (transactionDao.sumTransfersToday(con, sender.getId()).add(amount).compareTo(DAILY_TRANSFER_LIMIT) > 0) {
                    throw new BankException("Daily transfer limit of Rs." + DAILY_TRANSFER_LIMIT + " exceeded");
                }

                String senderName = userDao.findById(con, sender.getUserId()).getFullName();
                String receiverName = userDao.findById(con, receiver.getUserId()).getFullName();
                String remarks = (r.remarks() == null || r.remarks().isBlank()) ? "" : " - " + r.remarks().trim();
                String ref = newReference();

                debit(con, sender, amount, "TRANSFER", "Sent to " + receiverName + " (" + mask(receiver.getAccountNumber()) + ")" + remarks, ref);
                credit(con, receiver, amount, "TRANSFER", "Received from " + senderName + " (" + mask(sender.getAccountNumber()) + ")" + remarks, ref);

                con.commit();
                return new Receipt(ref, amount, sender.getBalance(), "Rs." + amount + " sent to " + receiverName);
            } catch (Exception e) {
                con.rollback();   // money is never lost: either both updates happen or none
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------ card & security

    public void setCardBlocked(long userId, boolean blocked) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            Account account = accountDao.findByUserId(con, userId);
            accountDao.updateCardBlocked(con, account.getId(), blocked);
        }
    }

    public void changePassword(long userId, Requests.ChangePassword r) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            User user = userDao.findById(con, userId);
            if (!authService.matches(r.oldPassword(), user.getPasswordHash())) throw new BankException("Current password is wrong");
            userDao.updatePassword(con, userId, authService.hash(r.newPassword()));
        }
    }

    public void changeMpin(long userId, Requests.ChangeMpin r) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            User user = userDao.findById(con, userId);
            if (!authService.matches(r.password(), user.getPasswordHash())) throw new BankException("Password is wrong");
            userDao.updateMpin(con, userId, authService.hash(r.newMpin()));
        }
    }

    // ------------------------------------------------------------------ shared helpers (used by other services)

    public void verifyMpin(Connection con, long userId, String mpin) throws SQLException {
        User user = userDao.findById(con, userId);
        if (!authService.matches(mpin, user.getMpinHash())) throw new BankException("Incorrect MPIN");
    }

    /** Finds the user's account and locks it for the current transaction. */
    public Account lockActiveAccount(Connection con, long userId) throws SQLException {
        Account account = accountDao.findByUserId(con, userId);
        if (account == null) throw new BankException("No bank account found");
        account = accountDao.findByIdForUpdate(con, account.getId());
        requireActive(account);
        return account;
    }

    /** Locks an account by its id (used by admin actions such as loan approval). */
    public Account lockAccountById(Connection con, long accountId) throws SQLException {
        Account account = accountDao.findByIdForUpdate(con, accountId);
        if (account == null) throw new BankException("Account not found");
        return account;
    }

    /** Adds money to the account and writes a CREDIT row in the passbook. */
    public Transaction credit(Connection con, Account account, BigDecimal amount, String category, String description, String ref) throws SQLException {
        BigDecimal newBalance = account.getBalance().add(amount);
        accountDao.updateBalance(con, account.getId(), newBalance);
        account.setBalance(newBalance);
        return saveTransaction(con, account, "CREDIT", category, amount, description, ref);
    }

    /** Removes money from the account (if enough balance) and writes a DEBIT row. */
    public Transaction debit(Connection con, Account account, BigDecimal amount, String category, String description, String ref) throws SQLException {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BankException("Insufficient balance. Available: Rs." + account.getBalance());
        }
        BigDecimal newBalance = account.getBalance().subtract(amount);
        accountDao.updateBalance(con, account.getId(), newBalance);
        account.setBalance(newBalance);
        return saveTransaction(con, account, "DEBIT", category, amount, description, ref);
    }

    public static String newReference() {
        return "TXN" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    /** Rounds to paise and checks the per-transaction limit. */
    public static BigDecimal checkAmount(BigDecimal amount) {
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        if (value.signum() <= 0) throw new BankException("Amount must be greater than 0");
        if (value.compareTo(MAX_PER_TRANSACTION) > 0) {
            throw new BankException("Maximum Rs." + MAX_PER_TRANSACTION + " allowed per transaction");
        }
        return value;
    }

    private void requireActive(Account account) {
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new BankException("Your account is frozen. Please contact your branch.");
        }
    }

    private Account findRecipient(Connection con, String to) throws SQLException {
        return to.contains("@") ? accountDao.findByUpiId(con, to.toLowerCase()) : accountDao.findByAccountNumber(con, to);
    }

    private Transaction saveTransaction(Connection con, Account account, String type, String category,
                                        BigDecimal amount, String description, String ref) throws SQLException {
        Transaction t = new Transaction();
        t.setAccountId(account.getId());
        t.setType(type);
        t.setCategory(category);
        t.setAmount(amount);
        t.setBalanceAfter(account.getBalance());
        t.setDescription(description);
        t.setReferenceNo(ref);
        transactionDao.insert(con, t);
        return t;
    }

    private static String mask(String accountNumber) {
        return "XXXX" + accountNumber.substring(accountNumber.length() - 4);
    }
}
