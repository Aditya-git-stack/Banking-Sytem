package com.bank.service;

import com.bank.dto.Receipt;
import com.bank.dto.Requests;
import com.bank.exception.BankException;
import com.bank.model.Account;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/** Bill payments and mobile / DTH recharge (simulated: money is debited from the account). */
@Service
public class BillService {

    private static final Set<String> BILL_TYPES = Set.of("ELECTRICITY", "WATER", "GAS", "DTH", "MOBILE");

    private final DataSource dataSource;
    private final AccountService accountService;

    public BillService(DataSource dataSource, AccountService accountService) {
        this.dataSource = dataSource;
        this.accountService = accountService;
    }

    public Receipt pay(long userId, Requests.PayBill r) throws SQLException {
        String type = r.billType().toUpperCase();
        if (!BILL_TYPES.contains(type)) throw new BankException("Unknown bill type: " + r.billType());
        BigDecimal amount = AccountService.checkAmount(r.amount());
        boolean isRecharge = type.equals("MOBILE") || type.equals("DTH");
        String description = isRecharge
                ? type + " recharge - " + r.operator() + " - " + r.consumerNumber()
                : type + " bill - " + r.operator() + " - " + r.consumerNumber();

        try (Connection con = dataSource.getConnection()) {
            accountService.verifyMpin(con, userId, r.mpin());
            con.setAutoCommit(false);
            try {
                Account account = accountService.lockActiveAccount(con, userId);
                String ref = AccountService.newReference();
                accountService.debit(con, account, amount, isRecharge ? "RECHARGE" : "BILL", description, ref);
                con.commit();
                return new Receipt(ref, amount, account.getBalance(), (isRecharge ? "Recharge" : "Bill payment") + " successful");
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }
}
