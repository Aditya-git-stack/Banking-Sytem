package com.example.bank.repository;

import com.example.bank.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring generates all the SQL (save, findById, ...) for us.
public interface AccountRepository extends JpaRepository<Account, Integer> {
}
