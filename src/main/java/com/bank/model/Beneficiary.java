package com.bank.model;


/**
 * One row of the 'beneficiaries' table: a saved payee for quick fund transfer.
 * A plain Java class (POJO): private fields + getters/setters.
 */
public class Beneficiary {

    private Long id;
    private Long userId;
    private String name;
    private String accountNumber;
    private String ifsc;
    private String nickname;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
