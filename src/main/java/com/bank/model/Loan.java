package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the 'loans' table.
 * A plain Java class (POJO): private fields + getters/setters.
 */
public class Loan {

    private Long id;
    private Long accountId;
    private String loanType;
    private BigDecimal principal;
    private BigDecimal interestRate;
    private int tenureMonths;
    private BigDecimal emi;
    private BigDecimal outstanding;
    private int emisPaid;
    private String status;
    private LocalDateTime appliedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }

    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public int getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(int tenureMonths) { this.tenureMonths = tenureMonths; }

    public BigDecimal getEmi() { return emi; }
    public void setEmi(BigDecimal emi) { this.emi = emi; }

    public BigDecimal getOutstanding() { return outstanding; }
    public void setOutstanding(BigDecimal outstanding) { this.outstanding = outstanding; }

    public int getEmisPaid() { return emisPaid; }
    public void setEmisPaid(int emisPaid) { this.emisPaid = emisPaid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
