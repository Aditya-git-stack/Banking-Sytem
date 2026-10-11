package com.example.bank.controller;

import com.example.bank.model.Account;
import com.example.bank.model.Transaction;
import com.example.bank.service.BankService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// REST API: the frontend talks to these URLs with JSON.
@RestController
@RequestMapping("/api")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    // POST /api/login   body: {"accountNumber":1001,"pin":"1234"}
    @PostMapping("/login")
    public Account login(@RequestBody Map<String, Object> body) {
        int accountNumber = Integer.parseInt(String.valueOf(body.get("accountNumber")));
        String pin = String.valueOf(body.get("pin"));
        return bankService.authenticate(accountNumber, pin);
    }

    // POST /api/accounts/1001/deposit   header: X-PIN   body: {"amount":500}
    @PostMapping("/accounts/{accountNumber}/deposit")
    public Account deposit(@PathVariable int accountNumber,
                           @RequestHeader("X-PIN") String pin,
                           @RequestBody Map<String, Double> body) {
        return bankService.deposit(accountNumber, pin, amountOf(body));
    }

    // POST /api/accounts/1001/withdraw
    @PostMapping("/accounts/{accountNumber}/withdraw")
    public Account withdraw(@PathVariable int accountNumber,
                            @RequestHeader("X-PIN") String pin,
                            @RequestBody Map<String, Double> body) {
        return bankService.withdraw(accountNumber, pin, amountOf(body));
    }

    // GET /api/accounts/1001/history
    @GetMapping("/accounts/{accountNumber}/history")
    public List<Transaction> history(@PathVariable int accountNumber,
                                     @RequestHeader("X-PIN") String pin) {
        return bankService.history(accountNumber, pin);
    }

    private double amountOf(Map<String, Double> body) {
        Double amount = body.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        return amount;
    }
}
