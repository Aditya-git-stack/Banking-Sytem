package com.example.bank.exception;

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
