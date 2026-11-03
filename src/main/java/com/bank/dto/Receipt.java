package com.bank.dto;

import java.math.BigDecimal;

/** What the frontend shows after any money movement (like the YONO success screen). */
public record Receipt(String referenceNo, BigDecimal amount, BigDecimal balance, String message) {
}
