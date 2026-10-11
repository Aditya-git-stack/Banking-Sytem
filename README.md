# Java Bank: Full Stack Banking App

A small full stack banking application.

| Layer    | Technology                                   |
|----------|----------------------------------------------|
| Frontend | HTML, CSS, JavaScript (`fetch` API)          |
| Backend  | Java 17, Spring Boot 3 (REST APIs)           |
| Database | H2 (file based) with Spring Data JPA/Hibernate |

## Features
- Login with account number and PIN
- Check balance
- Deposit and withdraw money
- Transaction history (saved in the database, survives restarts)
- Validation and clear error messages (wrong PIN, insufficient balance, invalid amount)

## How to run

**You need:** JDK 17 or newer and Maven, plus an internet connection the first time (to download dependencies).

```
mvn spring-boot:run
```

Open **http://localhost:8080** in your browser.

Demo logins: `1001` / `1234` and `1002` / `4321`.

Alternative: open the folder in IntelliJ/Eclipse/VS Code as a Maven project and run `BankApplication`.

To build a jar: `mvn package` then `java -jar target/bank-fullstack-1.0.0.jar`.

## Project structure

```
src/main/java/com/example/bank/
  BankApplication.java        entry point
  DataLoader.java             adds 2 demo accounts on first start
  model/                      Account, Transaction (database entities)
  repository/                 AccountRepository, TransactionRepository (JPA)
  service/BankService.java    business logic (login check, deposit, withdraw)
  controller/BankController.java   REST endpoints
  exception/                  custom exceptions + global error handler
src/main/resources/
  application.properties      server and database config
  static/index.html           the frontend
```

## REST API

The PIN is sent in the `X-PIN` header for protected endpoints.

| Method | URL                                   | Body                                  | Purpose        |
|--------|---------------------------------------|---------------------------------------|----------------|
| POST   | `/api/login`                          | `{"accountNumber":1001,"pin":"1234"}` | Log in         |
| POST   | `/api/accounts/{accNo}/deposit`       | `{"amount":500}`                      | Deposit        |
| POST   | `/api/accounts/{accNo}/withdraw`      | `{"amount":200}`                      | Withdraw       |
| GET    | `/api/accounts/{accNo}/history`       |                                       | List transactions |

Errors come back as JSON: `{"error": "Not enough balance. Available: 5000.0"}`

Quick test with curl:
```
curl -X POST localhost:8080/api/login -H "Content-Type: application/json" -d '{"accountNumber":1001,"pin":"1234"}'
curl -X POST localhost:8080/api/accounts/1001/deposit -H "Content-Type: application/json" -H "X-PIN: 1234" -d '{"amount":500}'
```

Browse the database at http://localhost:8080/h2-console after starting with the
environment variable `H2_CONSOLE=true` (it is off by default so it is not exposed online).
JDBC URL `jdbc:h2:file:./data/bankdb`, user `sa`, empty password.

## Deploy (Render, using the Dockerfile)
1. Push this repo to GitHub.
2. On render.com: New, Web Service, pick the repo, runtime Docker, free instance, Create.
3. Open the URL Render gives you. The free tier sleeps when idle, so the first load can be slow,
   and the H2 data resets when the service restarts.

## How it works (interview notes)

1. The browser loads `index.html` from Spring Boot's `static` folder.
2. JavaScript calls the REST API with `fetch` and sends/receives JSON.
3. `BankController` receives the request and calls `BankService`.
4. `BankService` applies the rules (PIN check, amount > 0, enough balance) and uses the repositories.
5. Spring Data JPA saves to the H2 database. No SQL is written by hand.
6. If a rule is broken, an exception is thrown and `GlobalExceptionHandler` turns it into a JSON error with the right HTTP status (401 or 400).

Key concepts used: OOP and encapsulation, layered architecture (controller, service, repository), dependency injection, JPA entities, REST, custom exceptions, `@Transactional` (deposit/withdraw update the balance and save the transaction together or not at all).

## Possible improvements
- Hash PINs (BCrypt) and use Spring Security with JWT or sessions
- Use `BigDecimal` instead of `double` for money
- Transfers between accounts and account registration
- Switch H2 to MySQL/PostgreSQL
- Unit tests for `BankService`
