package com.example.bank.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// Entity = a database table. Each field becomes a column.
@Entity
public class Account {

    @Id
    private int accountNumber;
    private String holderName;

    @JsonIgnore // never send the PIN to the frontend
    private String pin;

    private double balance;

    protected Account() { } // required by JPA

    public Account(int accountNumber, String holderName, String pin, double balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.pin = pin;
        this.balance = balance;
    }

    public int getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public boolean checkPin(String enteredPin) {
        return pin != null && pin.equals(enteredPin);
    }
}
