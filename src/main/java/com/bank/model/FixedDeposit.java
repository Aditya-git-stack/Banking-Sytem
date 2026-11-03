package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of the 'fixed_deposits' table.
 * A plain Java class (POJO): private fields + getters/setters.
 */
public class FixedDeposit {

    private Long id;
    private Long accountId;
    private BigDecimal principal;
    private BigDecimal interestRate;
    private int tenureMonths;
    private BigDecimal maturityAmount;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public int getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(int tenureMonths) { this.tenureMonths = tenureMonths; }

    public BigDecimal getMaturityAmount() { return maturityAmount; }
    public void setMaturityAmount(BigDecimal maturityAmount) { this.maturityAmount = maturityAmount; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
