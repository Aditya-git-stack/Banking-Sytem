package com.bank.controller;

import com.bank.service.AdminService;
import com.bank.service.LoanService;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Admin-only APIs. AuthInterceptor blocks non-admin users from /api/admin/**. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final LoanService loanService;

    public AdminController(AdminService adminService, LoanService loanService) {
        this.adminService = adminService;
        this.loanService = loanService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() throws SQLException {
        return adminService.stats();
    }

    @GetMapping("/customers")
    public List<Map<String, Object>> customers() throws SQLException {
        return adminService.customers();
    }

    @PostMapping("/accounts/{id}/freeze")
    public Map<String, String> freeze(@PathVariable long id) throws SQLException {
        adminService.setAccountStatus(id, "FROZEN");
        return Map.of("message", "Account frozen");
    }

    @PostMapping("/accounts/{id}/unfreeze")
    public Map<String, String> unfreeze(@PathVariable long id) throws SQLException {
        adminService.setAccountStatus(id, "ACTIVE");
        return Map.of("message", "Account activated");
    }

    @GetMapping("/transactions")
    public List<Map<String, Object>> transactions(@RequestParam(defaultValue = "50") int limit) throws SQLException {
        return adminService.transactions(Math.min(limit, 500));
    }

    @GetMapping("/loans")
    public List<Map<String, Object>> loans() throws SQLException {
        return adminService.loans();
    }

    @PostMapping("/loans/{id}/approve")
    public Map<String, String> approve(@PathVariable long id) throws SQLException {
        loanService.approve(id);
        return Map.of("message", "Loan approved and amount credited");
    }

    @PostMapping("/loans/{id}/reject")
    public Map<String, String> reject(@PathVariable long id) throws SQLException {
        loanService.reject(id);
        return Map.of("message", "Loan rejected");
    }
}
