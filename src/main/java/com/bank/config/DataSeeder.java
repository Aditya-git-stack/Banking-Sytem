package com.bank.config;

import com.bank.dto.Requests;
import com.bank.service.AccountService;
import com.bank.service.AuthService;
import com.bank.service.BeneficiaryService;
import com.bank.service.BillService;
import com.bank.service.LoanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Runs once when the app starts. If the database is empty it creates
 * demo users so you can log in immediately:
 *   admin / admin123          (bank admin)
 *   rahul / rahul123 MPIN 1234 (customer)
 *   priya / priya123 MPIN 1234 (customer)
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AuthService authService;
    private final AccountService accountService;
    private final BeneficiaryService beneficiaryService;
    private final BillService billService;
    private final LoanService loanService;

    public DataSeeder(AuthService authService, AccountService accountService, BeneficiaryService beneficiaryService,
                      BillService billService, LoanService loanService) {
        this.authService = authService;
        this.accountService = accountService;
        this.beneficiaryService = beneficiaryService;
        this.billService = billService;
        this.loanService = loanService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (authService.usernameExists("admin")) {
            return; // already seeded
        }
        authService.createAdmin("Bank Admin", "admin@bank.com", "9000000000", "admin", "admin123");

        long rahul = authService.register(new Requests.Register("Rahul Sharma", "rahul@gmail.com", "9876543210",
                "rahul", "rahul123", "1234", "SAVINGS"));
        long priya = authService.register(new Requests.Register("Priya Patel", "priya@gmail.com", "9876543211",
                "priya", "priya123", "1234", "SAVINGS"));

        accountService.deposit(rahul, new Requests.Amount(new BigDecimal("50000"), "1234"));
        accountService.deposit(priya, new Requests.Amount(new BigDecimal("75000"), "1234"));

        String priyaAccount = accountService.getAccountOfUser(priya).getAccountNumber();
        beneficiaryService.add(rahul, new Requests.AddBeneficiary("Priya Patel", priyaAccount, "SMBK0001234", "Sister"));
        accountService.transfer(rahul, new Requests.Transfer(priyaAccount, new BigDecimal("2500"), "Birthday gift", "1234"));
        billService.pay(rahul, new Requests.PayBill("ELECTRICITY", "TPCODL", "1100223344", new BigDecimal("1240"), "1234"));
        billService.pay(rahul, new Requests.PayBill("MOBILE", "Jio", "9876543210", new BigDecimal("299"), "1234"));
        accountService.withdraw(priya, new Requests.Amount(new BigDecimal("5000"), "1234"));
        loanService.apply(rahul, new Requests.ApplyLoan("PERSONAL", new BigDecimal("100000"), 24));

        log.info("Demo data created. Login with rahul/rahul123 (MPIN 1234) or admin/admin123");
    }
}
