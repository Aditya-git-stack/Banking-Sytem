package com.bank;

import com.bank.dto.Receipt;
import com.bank.dto.Requests;
import com.bank.exception.BankException;
import com.bank.service.AccountService;
import com.bank.service.AuthService;
import com.bank.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Starts the whole app with an in-memory DB and tests the main money flows. */
@SpringBootTest
class BankServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private LoanService loanService;

    private long newCustomer(String name, String mobile) throws Exception {
        return authService.register(new Requests.Register(name, name + "@test.com", mobile, name, "secret123", "1111", "SAVINGS"));
    }

    @Test
    void transferMovesMoneyBetweenAccounts() throws Exception {
        long a = newCustomer("alice", "9100000001");
        long b = newCustomer("bobby", "9100000002");
        accountService.deposit(a, new Requests.Amount(new BigDecimal("1000"), "1111"));

        String bAccount = accountService.getAccountOfUser(b).getAccountNumber();
        Receipt r = accountService.transfer(a, new Requests.Transfer(bAccount, new BigDecimal("300"), "test", "1111"));

        assertEquals(0, new BigDecimal("700").compareTo(r.balance()));
        assertEquals(0, new BigDecimal("300").compareTo(accountService.getAccountOfUser(b).getBalance()));
    }

    @Test
    void transferByUpiIdWorks() throws Exception {
        long a = newCustomer("carol", "9100000003");
        long b = newCustomer("david", "9100000004");
        accountService.deposit(a, new Requests.Amount(new BigDecimal("500"), "1111"));

        accountService.transfer(a, new Requests.Transfer("9100000004@bank", new BigDecimal("200"), null, "1111"));

        assertEquals(0, new BigDecimal("200").compareTo(accountService.getAccountOfUser(b).getBalance()));
    }

    @Test
    void insufficientBalanceIsRejectedAndNothingChanges() throws Exception {
        long a = newCustomer("emily", "9100000005");
        long b = newCustomer("frank", "9100000006");
        accountService.deposit(a, new Requests.Amount(new BigDecimal("100"), "1111"));
        String bAccount = accountService.getAccountOfUser(b).getAccountNumber();

        assertThrows(BankException.class, () ->
                accountService.transfer(a, new Requests.Transfer(bAccount, new BigDecimal("500"), null, "1111")));

        assertEquals(0, new BigDecimal("100").compareTo(accountService.getAccountOfUser(a).getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(accountService.getAccountOfUser(b).getBalance()));
    }

    @Test
    void wrongMpinIsRejected() throws Exception {
        long a = newCustomer("grace", "9100000007");
        BankException e = assertThrows(BankException.class, () ->
                accountService.deposit(a, new Requests.Amount(new BigDecimal("100"), "9999")));
        assertEquals("Incorrect MPIN", e.getMessage());
    }

    @Test
    void emiFormulaIsCorrect() {
        // 1,00,000 at 12% for 12 months -> EMI 8884.88 (standard bank value)
        assertEquals(new BigDecimal("8884.88"), loanService.calculateEmi(new BigDecimal("100000"), new BigDecimal("12"), 12));
    }
}
