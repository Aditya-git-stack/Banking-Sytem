package com.bank.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * One row of the 'users' table: a customer or an admin who can log in.
 * A plain Java class (POJO): private fields + getters/setters.
 */
public class User {

    private Long id;
    private String fullName;
    private String email;
    private String mobile;
    private String username;
    @JsonIgnore // never send hashes to the browser
    private String passwordHash;
    @JsonIgnore // never send hashes to the browser
    private String mpinHash;
    private String role;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getMpinHash() { return mpinHash; }
    public void setMpinHash(String mpinHash) { this.mpinHash = mpinHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
