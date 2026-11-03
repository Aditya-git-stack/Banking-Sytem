package com.bank.service;

import com.bank.dao.FixedDepositDao;
import com.bank.dto.Receipt;
import com.bank.dto.Requests;
import com.bank.exception.BankException;
import com.bank.model.Account;
import com.bank.model.FixedDeposit;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Fixed Deposits: money is moved from the savings balance into an FD and earns interest.
 * Interest is compounded every quarter: A = P * (1 + r/400) ^ (4 * years)
 */
@Service
public class FixedDepositService {

    private static final BigDecimal MIN_FD = new BigDecimal("1000");
    private static final BigDecimal PREMATURE_PENALTY = new BigDecimal("1.00"); // 1% less interest

    private final DataSource dataSource;
    private final FixedDepositDao fdDao;
    private final AccountService accountService;

    public FixedDepositService(DataSource dataSource, FixedDepositDao fdDao, AccountService accountService) {
        this.dataSource = dataSource;
        this.fdDao = fdDao;
        this.accountService = accountService;
    }

    /** Interest rate (% per year) depends on how long the money is kept. */
    public BigDecimal rateFor(int months) {
        if (months < 12) return new BigDecimal("6.00");
        if (months < 24) return new BigDecimal("6.80");
        if (months < 60) return new BigDecimal("7.00");
        return new BigDecimal("6.50");
    }

    public BigDecimal maturityAmount(BigDecimal principal, BigDecimal rate, int months) {
        double quarters = months / 3.0;
        double amount = principal.doubleValue() * Math.pow(1 + rate.doubleValue() / 400, quarters);
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, Object> preview(BigDecimal amount, int months) {
        BigDecimal rate = rateFor(months);
        return Map.of("interestRate", rate,
                "maturityAmount", maturityAmount(amount, rate, months),
                "maturityDate", LocalDate.now().plusMonths(months).toString());
    }

    public List<FixedDeposit> list(long userId) throws SQLException {
        long accountId = accountService.getAccountOfUser(userId).getId();
        try (Connection con = dataSource.getConnection()) {
            return fdDao.findByAccountId(con, accountId);
        }
    }

    public Receipt open(long userId, Requests.OpenFd r) throws SQLException {
        BigDecimal amount = AccountService.checkAmount(r.amount());
        if (amount.compareTo(MIN_FD) < 0) throw new BankException("Minimum FD amount is Rs." + MIN_FD);

        try (Connection con = dataSource.getConnection()) {
            accountService.verifyMpin(con, userId, r.mpin());
            con.setAutoCommit(false);
            try {
                Account account = accountService.lockActiveAccount(con, userId);
                BigDecimal rate = rateFor(r.tenureMonths());
                String ref = AccountService.newReference();
                accountService.debit(con, account, amount, "FD",
                        "Fixed deposit opened (" + r.tenureMonths() + " months @ " + rate + "%)", ref);

                FixedDeposit fd = new FixedDeposit();
                fd.setAccountId(account.getId());
                fd.setPrincipal(amount);
                fd.setInterestRate(rate);
                fd.setTenureMonths(r.tenureMonths());
                fd.setMaturityAmount(maturityAmount(amount, rate, r.tenureMonths()));
                fd.setStartDate(LocalDate.now());
                fd.setMaturityDate(LocalDate.now().plusMonths(r.tenureMonths()));
                fd.setStatus("ACTIVE");
                fdDao.insert(con, fd);

                con.commit();
                return new Receipt(ref, amount, account.getBalance(), "Fixed deposit opened successfully");
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Closes an FD. Before maturity: simple interest at (rate - 1%) for the months completed. */
    public Receipt close(long userId, long fdId, String mpin) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            accountService.verifyMpin(con, userId, mpin);
            con.setAutoCommit(false);
            try {
                Account account = accountService.lockActiveAccount(con, userId);
                FixedDeposit fd = fdDao.findById(con, fdId);
                if (fd == null || fd.getAccountId() != account.getId().longValue()) throw new BankException("FD not found");
                if (!"ACTIVE".equals(fd.getStatus())) throw new BankException("FD is already closed");

                BigDecimal payout;
                if (!LocalDate.now().isBefore(fd.getMaturityDate())) {
                    payout = fd.getMaturityAmount();
                } else {
                    long monthsDone = ChronoUnit.MONTHS.between(fd.getStartDate(), LocalDate.now());
                    BigDecimal rate = fd.getInterestRate().subtract(PREMATURE_PENALTY).max(BigDecimal.ZERO);
                    BigDecimal interest = fd.getPrincipal().multiply(rate).multiply(BigDecimal.valueOf(monthsDone))
                            .divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP);
                    payout = fd.getPrincipal().add(interest);
                }

                String ref = AccountService.newReference();
                accountService.credit(con, account, payout, "FD", "Fixed deposit #" + fd.getId() + " closed", ref);
                fdDao.updateStatus(con, fd.getId(), "CLOSED");

                con.commit();
                return new Receipt(ref, payout, account.getBalance(), "FD closed. Rs." + payout + " credited to your account");
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }
}
