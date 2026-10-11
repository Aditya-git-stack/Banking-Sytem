package com.example.bank.exception;

// Custom exception for the "not enough balance" business rule.
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
