package com.bank.controller;

import com.bank.dto.Receipt;
import com.bank.dto.Requests;
import com.bank.model.Transaction;
import com.bank.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Balance, deposit, withdraw, transfer, statement, debit card and security settings. */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/deposit")
    public Receipt deposit(@RequestAttribute("userId") long userId, @Valid @RequestBody Requests.Amount request) throws SQLException {
        return accountService.deposit(userId, request);
    }

    @PostMapping("/withdraw")
    public Receipt withdraw(@RequestAttribute("userId") long userId, @Valid @RequestBody Requests.Amount request) throws SQLException {
        return accountService.withdraw(userId, request);
    }

    @GetMapping("/lookup")
    public Map<String, String> lookup(@RequestParam String to) throws SQLException {
        return accountService.lookupRecipient(to.trim());
    }

    @PostMapping("/transfer")
    public Receipt transfer(@RequestAttribute("userId") long userId, @Valid @RequestBody Requests.Transfer request) throws SQLException {
        return accountService.transfer(userId, request);
    }

    @GetMapping("/transactions")
    public List<Transaction> recent(@RequestAttribute("userId") long userId,
                                    @RequestParam(defaultValue = "10") int limit) throws SQLException {
        return accountService.miniStatement(userId, Math.min(limit, 100));
    }

    @GetMapping("/statement")
    public List<Transaction> statement(@RequestAttribute("userId") long userId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws SQLException {
        return accountService.statement(userId, from, to);
    }

    @PostMapping("/card/block")
    public Map<String, String> blockCard(@RequestAttribute("userId") long userId) throws SQLException {
        accountService.setCardBlocked(userId, true);
        return Map.of("message", "Debit card blocked");
    }

    @PostMapping("/card/unblock")
    public Map<String, String> unblockCard(@RequestAttribute("userId") long userId) throws SQLException {
        accountService.setCardBlocked(userId, false);
        return Map.of("message", "Debit card unblocked");
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@RequestAttribute("userId") long userId,
                                              @Valid @RequestBody Requests.ChangePassword request) throws SQLException {
        accountService.changePassword(userId, request);
        return Map.of("message", "Password changed successfully");
    }

    @PostMapping("/change-mpin")
    public Map<String, String> changeMpin(@RequestAttribute("userId") long userId,
                                          @Valid @RequestBody Requests.ChangeMpin request) throws SQLException {
        accountService.changeMpin(userId, request);
        return Map.of("message", "MPIN changed successfully");
    }
}
