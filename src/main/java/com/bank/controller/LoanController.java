package com.bank.controller;

import com.bank.dto.Receipt;
import com.bank.dto.Requests;
import com.bank.model.Loan;
import com.bank.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /** Public (no login needed): calculate EMI before applying. */
    @GetMapping("/emi-calculator")
    public Map<String, Object> emi(@RequestParam String loanType, @RequestParam BigDecimal amount, @RequestParam int months) {
        return loanService.emiCalculator(loanType, amount, months);
    }

    @GetMapping
    public List<Loan> myLoans(@RequestAttribute("userId") long userId) throws SQLException {
        return loanService.myLoans(userId);
    }

    @PostMapping
    public Map<String, String> apply(@RequestAttribute("userId") long userId, @Valid @RequestBody Requests.ApplyLoan request) throws SQLException {
        loanService.apply(userId, request);
        return Map.of("message", "Loan application submitted. Waiting for bank approval.");
    }

    @PostMapping("/{id}/pay-emi")
    public Receipt payEmi(@RequestAttribute("userId") long userId, @PathVariable long id,
                          @Valid @RequestBody Requests.Mpin request) throws SQLException {
        return loanService.payEmi(userId, id, request.mpin());
    }
}
