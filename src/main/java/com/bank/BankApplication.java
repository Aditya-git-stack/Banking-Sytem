package com.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starting point of the application. Run this class (or press F5 in VS Code).
 * It starts an embedded web server on http://localhost:8080 that serves
 * both the REST APIs (/api/...) and the frontend pages (from resources/static).
 */
@SpringBootApplication
public class BankApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankApplication.class, args);
    }
}
