package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the 'accounts' table: the bank account of a customer (balance, UPI id, debit card).
 * A plain Java class (POJO): private fields + getters/setters.
 */
public class Account {

    private Long id;
    private Long userId;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String status;
    private String ifsc;
    private String branch;
    private String upiId;
    private String cardNumber;
    private boolean cardBlocked;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public boolean isCardBlocked() { return cardBlocked; }
    public void setCardBlocked(boolean cardBlocked) { this.cardBlocked = cardBlocked; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
