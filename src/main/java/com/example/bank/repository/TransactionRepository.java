package com.example.bank.repository;

import com.example.bank.model.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Spring builds the query from the method name.
    List<Transaction> findByAccountNumberOrderByIdDesc(int accountNumber);
}
