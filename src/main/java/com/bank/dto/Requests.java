package com.bank.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * All request bodies (JSON sent by the frontend) in one file.
 * A Java 'record' is a short way to write a class with fields, constructor and getters.
 * The annotations (@NotBlank, @Positive ...) are checked automatically when a controller uses @Valid.
 */
public final class Requests {

    private Requests() {
    }

    public record Register(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{10}", message = "must be 10 digits") String mobile,
            @NotBlank @Size(min = 4, max = 20) String username,
            @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password,
            @NotBlank @Pattern(regexp = "\\d{4}", message = "must be 4 digits") String mpin,
            @NotBlank String accountType) {
    }

    public record Login(@NotBlank String username, @NotBlank String password) {
    }

    /** Used by deposit and withdraw. */
    public record Amount(@NotNull @Positive BigDecimal amount, @NotBlank String mpin) {
    }

    /** 'to' can be an account number OR a UPI id (e.g. 9876543211@bank). */
    public record Transfer(@NotBlank String to, @NotNull @Positive BigDecimal amount, String remarks, @NotBlank String mpin) {
    }

    public record AddBeneficiary(@NotBlank String name, @NotBlank String accountNumber, @NotBlank String ifsc, String nickname) {
    }

    public record OpenFd(@NotNull @Positive BigDecimal amount, @Min(3) @Max(120) int tenureMonths, @NotBlank String mpin) {
    }

    public record ApplyLoan(@NotBlank String loanType, @NotNull @Positive BigDecimal amount, @Min(6) @Max(360) int tenureMonths) {
    }

    /** billType: ELECTRICITY, WATER, GAS, DTH, MOBILE */
    public record PayBill(@NotBlank String billType, @NotBlank String operator, @NotBlank String consumerNumber,
                          @NotNull @Positive BigDecimal amount, @NotBlank String mpin) {
    }

    public record Mpin(@NotBlank String mpin) {
    }

    public record ChangePassword(@NotBlank String oldPassword, @NotBlank @Size(min = 6) String newPassword) {
    }

    public record ChangeMpin(@NotBlank String password, @NotBlank @Pattern(regexp = "\\d{4}", message = "must be 4 digits") String newMpin) {
    }
}
