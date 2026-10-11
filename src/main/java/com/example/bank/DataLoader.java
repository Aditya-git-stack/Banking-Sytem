package com.example.bank;

import com.example.bank.model.Account;
import com.example.bank.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Adds two demo accounts the first time the app starts.
@Component
public class DataLoader implements CommandLineRunner {

    private final AccountRepository accountRepository;

    public DataLoader(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.count() == 0) {
            accountRepository.save(new Account(1001, "Rahul", "1234", 5000));
            accountRepository.save(new Account(1002, "Priya", "4321", 10000));
        }
    }
}
