package com.example.bank.service;

import com.example.bank.exception.AuthException;
import com.example.bank.exception.InsufficientFundsException;
import com.example.bank.model.Account;
import com.example.bank.model.Transaction;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.TransactionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Business logic lives here (the same rules as the console version).
@Service
public class BankService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public BankService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    // Finds the account and checks the PIN. Throws if either is wrong.
    public Account authenticate(int accountNumber, String pin) {
        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AuthException("Invalid account number or PIN"));
        if (!account.checkPin(pin)) {
            throw new AuthException("Invalid account number or PIN");
        }
        return account;
    }

    @Transactional
    public Account deposit(int accountNumber, String pin, double amount) {
        Account account = authenticate(accountNumber, pin);
        validateAmount(amount);
        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);
        transactionRepository.save(new Transaction(accountNumber, "DEPOSIT", amount, account.getBalance()));
        return account;
    }

    @Transactional
    public Account withdraw(int accountNumber, String pin, double amount) {
        Account account = authenticate(accountNumber, pin);
        validateAmount(amount);
        if (amount > account.getBalance()) {
            throw new InsufficientFundsException("Not enough balance. Available: " + account.getBalance());
        }
        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);
        transactionRepository.save(new Transaction(accountNumber, "WITHDRAW", amount, account.getBalance()));
        return account;
    }

    public List<Transaction> history(int accountNumber, String pin) {
        authenticate(accountNumber, pin);
        return transactionRepository.findByAccountNumberOrderByIdDesc(accountNumber);
    }

    private void validateAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
    }
}
