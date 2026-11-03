package com.bank.exception;

/**
 * Thrown for any business-rule failure, e.g. "Insufficient balance" or "Wrong MPIN".
 * GlobalExceptionHandler turns it into an HTTP 400 response with the message.
 */
public class BankException extends RuntimeException {
    public BankException(String message) {
        super(message);
    }
}
