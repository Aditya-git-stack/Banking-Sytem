package com.bank.service;

import com.bank.dao.AccountDao;
import com.bank.dao.LoanDao;
import com.bank.dao.TransactionDao;
import com.bank.dao.UserDao;
import com.bank.exception.BankException;
import com.bank.model.Account;
import com.bank.model.Loan;
import com.bank.model.Transaction;
import com.bank.model.User;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything the bank admin (manager) can see and do. */
@Service
public class AdminService {

    private final DataSource dataSource;
    private final UserDao userDao;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final LoanDao loanDao;

    public AdminService(DataSource dataSource, UserDao userDao, AccountDao accountDao,
                        TransactionDao transactionDao, LoanDao loanDao) {
        this.dataSource = dataSource;
        this.userDao = userDao;
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.loanDao = loanDao;
    }

    public Map<String, Object> stats() throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("customers", userDao.findAllCustomers(con).size());
            stats.put("totalDeposits", accountDao.totalBalance(con));
            stats.put("transactions", transactionDao.count(con));
            stats.put("pendingLoans", loanDao.countByStatus(con, "PENDING"));
            return stats;
        }
    }

    /** Each customer together with their account. */
    public List<Map<String, Object>> customers() throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (User user : userDao.findAllCustomers(con)) {
                Map<String, Object> row = new HashMap<>();
                row.put("user", user);
                row.put("account", accountDao.findByUserId(con, user.getId()));
                result.add(row);
            }
            return result;
        }
    }

    public void setAccountStatus(long accountId, String status) throws SQLException {
        if (!status.equals("ACTIVE") && !status.equals("FROZEN")) throw new BankException("Invalid status");
        try (Connection con = dataSource.getConnection()) {
            accountDao.updateStatus(con, accountId, status);
        }
    }

    /** Recent transactions of the whole bank, with the account number added for display. */
    public List<Map<String, Object>> transactions(int limit) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            Map<Long, String> accountNumbers = accountNumberMap(con);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Transaction t : transactionDao.findAllRecent(con, limit)) {
                result.add(Map.of("transaction", t, "accountNumber", accountNumbers.get(t.getAccountId())));
            }
            return result;
        }
    }

    /** All loans with the customer's name and account number. */
    public List<Map<String, Object>> loans() throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Loan loan : loanDao.findAll(con)) {
                Account account = accountDao.findById(con, loan.getAccountId());
                User owner = userDao.findById(con, account.getUserId());
                result.add(Map.of("loan", loan, "accountNumber", account.getAccountNumber(), "customerName", owner.getFullName()));
            }
            return result;
        }
    }

    private Map<Long, String> accountNumberMap(Connection con) throws SQLException {
        Map<Long, String> map = new HashMap<>();
        for (Account a : accountDao.findAll(con)) map.put(a.getId(), a.getAccountNumber());
        return map;
    }
}
