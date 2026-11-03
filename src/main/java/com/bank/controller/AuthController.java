package com.bank.controller;

import com.bank.dto.Requests;
import com.bank.service.AccountService;
import com.bank.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Map;

/**
 * Controller = receives HTTP requests from the browser and returns JSON.
 * It contains no business logic: it only calls the service.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;

    public AuthController(AuthService authService, AccountService accountService) {
        this.authService = authService;
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody Requests.Register request) throws SQLException {
        authService.register(request);
        return Map.of("message", "Account opened successfully. Please login.");
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody Requests.Login request) throws SQLException {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader("Authorization") String header) {
        authService.logout(header.replace("Bearer ", ""));
        return Map.of("message", "Logged out");
    }

    /** Details of the logged-in user + their account. */
    @GetMapping("/me")
    public Map<String, Object> me(@RequestAttribute("userId") long userId) throws SQLException {
        return accountService.getProfile(userId);
    }
}
