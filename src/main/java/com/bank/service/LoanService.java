package com.bank.service;

import com.bank.dao.LoanDao;
import com.bank.dto.Receipt;
import com.bank.dto.Requests;
import com.bank.exception.BankException;
import com.bank.model.Account;
import com.bank.model.Loan;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Loans: customer applies -> admin approves (money credited) -> customer pays EMIs until CLOSED.
 * EMI formula: EMI = P * r * (1+r)^n / ((1+r)^n - 1), r = monthly rate, n = months
 */
@Service
public class LoanService {

    public static final Map<String, BigDecimal> RATES = Map.of(
            "HOME", new BigDecimal("8.50"),
            "CAR", new BigDecimal("9.20"),
            "PERSONAL", new BigDecimal("11.50"),
            "EDUCATION", new BigDecimal("8.15"));

    private static final BigDecimal MAX_LOAN = new BigDecimal("5000000");

    private final DataSource dataSource;
    private final LoanDao loanDao;
    private final AccountService accountService;

    public LoanService(DataSource dataSource, LoanDao loanDao, AccountService accountService) {
        this.dataSource = dataSource;
        this.loanDao = loanDao;
        this.accountService = accountService;
    }

    public BigDecimal rateFor(String loanType) {
        BigDecimal rate = RATES.get(loanType.toUpperCase());
        if (rate == null) throw new BankException("Loan type must be HOME, CAR, PERSONAL or EDUCATION");
        return rate;
    }

    public BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int months) {
        double p = principal.doubleValue();
        double r = annualRate.doubleValue() / 12 / 100;
        double emi = p * r * Math.pow(1 + r, months) / (Math.pow(1 + r, months) - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, Object> emiCalculator(String loanType, BigDecimal amount, int months) {
        if (months <= 0 || amount.signum() <= 0) throw new BankException("Amount and months must be positive");
        BigDecimal rate = rateFor(loanType);
        BigDecimal emi = calculateEmi(amount, rate, months);
        BigDecimal total = emi.multiply(BigDecimal.valueOf(months));
        return Map.of("interestRate", rate, "emi", emi, "totalPayable", total, "totalInterest", total.subtract(amount));
    }

    public List<Loan> myLoans(long userId) throws SQLException {
        long accountId = accountService.getAccountOfUser(userId).getId();
        try (Connection con = dataSource.getConnection()) {
            return loanDao.findByAccountId(con, accountId);
        }
    }

    public void apply(long userId, Requests.ApplyLoan r) throws SQLException {
        BigDecimal amount = r.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(MAX_LOAN) > 0) throw new BankException("Maximum loan amount is Rs." + MAX_LOAN);
        BigDecimal rate = rateFor(r.loanType());
        BigDecimal emi = calculateEmi(amount, rate, r.tenureMonths());

        Loan loan = new Loan();
        loan.setAccountId(accountService.getAccountOfUser(userId).getId());
        loan.setLoanType(r.loanType().toUpperCase());
        loan.setPrincipal(amount);
        loan.setInterestRate(rate);
        loan.setTenureMonths(r.tenureMonths());
        loan.setEmi(emi);
        loan.setOutstanding(emi.multiply(BigDecimal.valueOf(r.tenureMonths())));
        loan.setStatus("PENDING");
        try (Connection con = dataSource.getConnection()) {
            loanDao.insert(con, loan);
        }
    }

    public Receipt payEmi(long userId, long loanId, String mpin) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            accountService.verifyMpin(con, userId, mpin);
            con.setAutoCommit(false);
            try {
                Account account = accountService.lockActiveAccount(con, userId);
                Loan loan = loanDao.findById(con, loanId);
                if (loan == null || loan.getAccountId() != account.getId().longValue()) throw new BankException("Loan not found");
                if (!"APPROVED".equals(loan.getStatus())) throw new BankException("EMI can be paid only for an active (approved) loan");

                BigDecimal pay = loan.getEmi().min(loan.getOutstanding());
                String ref = AccountService.newReference();
                accountService.debit(con, account, pay, "LOAN",
                        "EMI " + (loan.getEmisPaid() + 1) + "/" + loan.getTenureMonths() + " - " + loan.getLoanType() + " loan #" + loan.getId(), ref);

                BigDecimal outstanding = loan.getOutstanding().subtract(pay);
                int emisPaid = loan.getEmisPaid() + 1;
                String status = (outstanding.signum() <= 0 || emisPaid >= loan.getTenureMonths()) ? "CLOSED" : "APPROVED";
                loanDao.updateRepayment(con, loan.getId(), outstanding.max(BigDecimal.ZERO), emisPaid, status);

                con.commit();
                return new Receipt(ref, pay, account.getBalance(),
                        "CLOSED".equals(status) ? "Final EMI paid. Loan closed!" : "EMI paid successfully");
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ---------------- admin actions ----------------

    /** Admin approves: the loan amount is credited to the customer's account. */
    public void approve(long loanId) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try {
                Loan loan = requirePending(con, loanId);
                Account account = accountService.lockAccountById(con, loan.getAccountId());
                accountService.credit(con, account, loan.getPrincipal(), "LOAN",
                        loan.getLoanType() + " loan #" + loan.getId() + " disbursed", AccountService.newReference());
                loanDao.updateStatus(con, loanId, "APPROVED");
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void reject(long loanId) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            requirePending(con, loanId);
            loanDao.updateStatus(con, loanId, "REJECTED");
        }
    }

    private Loan requirePending(Connection con, long loanId) throws SQLException {
        Loan loan = loanDao.findById(con, loanId);
        if (loan == null) throw new BankException("Loan not found");
        if (!"PENDING".equals(loan.getStatus())) throw new BankException("Loan is already " + loan.getStatus());
        return loan;
    }
}
