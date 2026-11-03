# Bank Management System (YONO-style)

A full stack banking app that works like the **SBI YONO** app.

| Layer    | Technology |
|----------|------------|
| Frontend | HTML, CSS, JavaScript (no framework) |
| Backend  | Java 17 + Spring Boot 3 (REST APIs) |
| Database | **Plain JDBC** (Connection, PreparedStatement, ResultSet) with H2. You can switch to MySQL |
| Security | BCrypt password/MPIN hashing, login token, MPIN for every payment |

---

## 1. Features

**Customer (like YONO)**
- Open an account online. You get an 11-digit account number, a UPI ID (`mobile@bank`), and a debit card
- Login and logout, with the balance hidden until you tap the eye icon
- **Send money** by account number **or UPI ID**. The payee name is checked first, every payment needs a 4-digit MPIN, and there are per-transaction and daily limits
- Beneficiaries: add, delete, and pay quickly
- Deposit and withdraw
- **Bill payments and recharge**: electricity, water, gas, DTH, mobile
- **Fixed deposits**: a live maturity calculator, quarterly compounding, and early closure with a penalty
- **Loans**: EMI calculator, apply for a loan, admin approval, the money is credited, then you pay EMIs until the loan is closed
- **Debit card**: block or unblock
- **Statement**: filter by date, download as CSV, print
- Profile, change password, change MPIN

**Admin (bank manager)**
- Dashboard stats: customers, total deposits, transactions, pending loans
- See all customers and accounts, and **freeze or unfreeze** any account
- **Approve or reject loans**
- See every transaction in the bank

---

## 2. Run it in VS Code (5 minutes)

1. Install **Java 17** (or newer): https://adoptium.net
2. Install **VS Code** and the extension **"Extension Pack for Java"** (VS Code suggests it when you open the folder).
3. `File → Open Folder…` → select this project folder.
4. Open `src/main/java/com/bank/BankApplication.java` and click **Run** above `main` (or press **F5**).
   - Or use the terminal: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
5. Open **http://localhost:8080** in your browser.

You do **not** need to install Maven or MySQL. The Maven wrapper (`mvnw`) and the built-in H2 database take care of that.

### Demo logins (created automatically on first start)
| Role | Username | Password | MPIN |
|------|----------|----------|------|
| Customer | `rahul` | `rahul123` | `1234` |
| Customer | `priya` | `priya123` | `1234` |
| Admin | `admin` | `admin123` | – |

Try this: log in as **rahul** and send money to `9876543211@bank` (Priya's UPI ID). Then log in as **admin** and approve Rahul's pending loan.

To see the database tables: open http://localhost:8080/h2-console with JDBC URL `jdbc:h2:file:./data/bankdb`, user `sa`, and an empty password.
To reset all data, stop the app and delete the `data` folder.

Run the tests with `./mvnw test`.

---

## 3. Project structure

```
src/main/java/com/bank
├── BankApplication.java      ← main() – starts everything
├── controller/               ← receives HTTP requests, returns JSON (no logic)
├── service/                  ← business rules: balance check, MPIN, limits, interest, EMI
├── dao/                      ← ONLY place with SQL (JDBC code)
├── model/                    ← plain Java classes = one table row (User, Account, Transaction…)
├── dto/                      ← shape of JSON the frontend sends (Requests) / receives (Receipt)
├── config/                   ← login check (AuthInterceptor), demo data (DataSeeder)
└── exception/                ← BankException + one global error handler

src/main/resources
├── application.properties    ← port, database settings
├── schema.sql                ← CREATE TABLE statements (runs at startup)
└── static/                   ← the frontend (served by Spring Boot)
    ├── index.html            ← login
    ├── register.html         ← open account
    ├── dashboard.html        ← customer app (YONO-style)
    ├── admin.html            ← admin panel
    ├── css/style.css
    └── js/api.js, dashboard.js, admin.js
```

This is called a **layered architecture**. Each layer talks only to the layer below it:

```
Browser (HTML/JS)  →  Controller  →  Service  →  DAO  →  Database
     ↑                                                     |
     └──────────────────── JSON response ←─────────────────┘
```

---

## 4. Spring Boot from zero (what you must know)

Spring Boot is a Java library that does the boring work for you. It starts a web server, turns Java objects into JSON, and creates objects and connects them together.

| Annotation | Meaning in simple words |
|------------|-------------------------|
| `@SpringBootApplication` | "This is the main class. Start Spring here." |
| `@RestController` | "This class handles web requests and returns JSON." |
| `@RequestMapping("/api/account")` | All URLs in this class start with `/api/account`. |
| `@GetMapping` / `@PostMapping` | This method runs for a GET or POST request on that URL. |
| `@RequestBody` | Convert the incoming JSON into a Java object. |
| `@RequestParam` | Read `?from=2025-01-01` from the URL. |
| `@PathVariable` | Read `{id}` from a URL like `/api/fd/5/close`. |
| `@RequestAttribute("userId")` | Read the logged-in user's id (set by our AuthInterceptor). |
| `@Valid` | Check the rules on the DTO (`@NotBlank`, `@Positive`, `@Pattern`…). |
| `@Service` | "This class has business logic." Spring creates one object of it. |
| `@Repository` | "This class talks to the database (DAO)." |
| `@Component` / `@Configuration` | A general class that Spring should create and manage. |
| `@RestControllerAdvice` | Catches errors from all controllers in one place. |

**Dependency Injection (DI):** we never write `new AccountService(...)`. We ask for it in the constructor and Spring passes the object in:
```java
public AccountController(AccountService accountService) {   // Spring passes it in
    this.accountService = accountService;
}
```
Why this helps: classes are loosely coupled, easy to test, and only one object of each is shared.

---

## 5. JDBC explained (the database part)

Spring Boot gives us a `DataSource`, which is a **pool of ready database connections** (HikariCP). The rest is pure JDBC:

```java
try (Connection con = dataSource.getConnection();                         // 1. get a connection
     PreparedStatement ps = con.prepareStatement(
         "SELECT * FROM accounts WHERE account_number = ?")) {             // 2. SQL with ? placeholders
    ps.setString(1, accountNumber);                                       // 3. fill the ?
    try (ResultSet rs = ps.executeQuery()) {                              // 4. run it
        if (rs.next()) { ... rs.getBigDecimal("balance") ... }            // 5. read the row
    }
}                                                                         // 6. auto-closed (try-with-resources)
```

- **PreparedStatement with `?`** prevents **SQL injection**, because user input is never joined into the SQL string.
- **try-with-resources** closes the connection automatically, even if there is an error.
- `executeQuery()` is for SELECT. `executeUpdate()` is for INSERT, UPDATE, and DELETE.
- `Statement.RETURN_GENERATED_KEYS` gives back the new auto-increment id after an INSERT.

### Transactions: the most important banking concept
A transfer has two updates: **debit the sender** and **credit the receiver**. Both must happen, or neither. See `AccountService.transfer()`:

```java
con.setAutoCommit(false);        // start transaction
try {
    debit(sender ...);           // UPDATE + INSERT
    credit(receiver ...);        // UPDATE + INSERT
    con.commit();                // save everything
} catch (Exception e) {
    con.rollback();              // undo everything, money is never lost
    throw e;
}
```
This gives the **ACID** properties (Atomicity, Consistency, Isolation, Durability).

### Concurrency (two payments at the same moment)
`SELECT ... FOR UPDATE` **locks the account row** until commit. A second payment has to wait, so it cannot read the old balance, and money cannot be spent twice.
We always lock the **smaller account id first**, so A→B and B→A running at the same moment cannot block each other forever (no **deadlock**).

---

## 6. How one request flows (example: Send Money)

1. The user clicks **Proceed to Pay** in `dashboard.html`. `dashboard.js` asks for the MPIN.
2. JS calls `api("POST", "/api/account/transfer", {to, amount, remarks, mpin})`. The token goes in the `Authorization` header.
3. **AuthInterceptor** checks the token, finds the user, and sets `userId`.
4. **AccountController.transfer()** receives the JSON as `Requests.Transfer`. `@Valid` checks it.
5. **AccountService.transfer()** checks the MPIN, finds the receiver, locks both rows, checks the status, limits, and balance, then debits, credits, and commits.
6. **AccountDao / TransactionDao** run the SQL with JDBC.
7. A `Receipt` comes back as JSON, and JS shows the green success popup.
8. If anything fails, a `BankException` is thrown. **GlobalExceptionHandler** returns `400 {"message": "..."}`, and JS shows a red toast.

---

## 7. Database tables

| Table | Purpose |
|-------|---------|
| `users` | login details, BCrypt password hash, MPIN hash, role (CUSTOMER/ADMIN) |
| `accounts` | account number, balance, status (ACTIVE/FROZEN), UPI ID, card |
| `transactions` | passbook: every CREDIT/DEBIT with the balance after it |
| `beneficiaries` | saved payees |
| `fixed_deposits` | FD amount, rate, maturity |
| `loans` | loan amount, EMI, outstanding, status |

Relations: one **user** has one **account**. One account has many **transactions**, **FDs**, and **loans**. One user has many **beneficiaries**.

---

## 8. REST API list

| Method | URL | What it does |
|--------|-----|--------------|
| POST | `/api/auth/register` | open account |
| POST | `/api/auth/login` | login → token |
| POST | `/api/auth/logout` | logout |
| GET | `/api/auth/me` | my profile + account |
| POST | `/api/account/deposit` | deposit |
| POST | `/api/account/withdraw` | withdraw |
| GET | `/api/account/lookup?to=` | verify payee name |
| POST | `/api/account/transfer` | send money |
| GET | `/api/account/transactions?limit=` | mini statement |
| GET | `/api/account/statement?from=&to=` | full statement |
| POST | `/api/account/card/block` · `/unblock` | debit card |
| POST | `/api/account/change-password` · `/change-mpin` | security |
| GET/POST/DELETE | `/api/beneficiaries` | beneficiaries |
| POST | `/api/bills/pay` | bill / recharge |
| GET | `/api/fd/preview` · GET/POST `/api/fd` · POST `/api/fd/{id}/close` | fixed deposits |
| GET | `/api/loans/emi-calculator` · GET/POST `/api/loans` · POST `/api/loans/{id}/pay-emi` | loans |
| GET | `/api/admin/stats`, `/customers`, `/loans`, `/transactions` | admin |
| POST | `/api/admin/accounts/{id}/freeze` · `/unfreeze` | admin |
| POST | `/api/admin/loans/{id}/approve` · `/reject` | admin |

---

## 9. Formulas used

- **EMI** = P × r × (1+r)ⁿ / ((1+r)ⁿ − 1), where r = yearly rate / 12 / 100 and n = number of months
- **FD maturity** (compounded every quarter) = P × (1 + rate/400)^(months/3)
- **FD closed early**: simple interest at (rate − 1%) for the months completed

Money is always stored as `BigDecimal` / `DECIMAL(15,2)`, **never `double`**. A `double` cannot store values like 0.1 exactly.

---

## 10. Explain the project in 1 minute (for the interviewer)

> "I built a YONO-style bank management system. The frontend is HTML, CSS and JavaScript. It calls REST APIs written in Spring Boot. The backend has three layers: controllers take requests, services hold the banking rules, and DAOs talk to the database using plain JDBC with PreparedStatements. Customers can open an account, transfer money by account number or UPI ID, pay bills, open FDs, apply for loans and download statements. An admin can freeze accounts and approve loans. For safety, passwords and MPINs are stored as BCrypt hashes, and every payment needs the MPIN. A fund transfer runs inside one JDBC transaction with commit and rollback, and uses SELECT FOR UPDATE row locks so two payments can't spend the same money."

## 11. Common interview questions and answers

**Q: Why JDBC and not Hibernate/JPA?**
JDBC lets me write and control the exact SQL. It is fast, and it shows I understand what happens inside the database. JPA writes the SQL automatically, which saves time on big projects.

**Q: How do you stop SQL injection?**
I use `PreparedStatement` with `?` placeholders everywhere, so input is sent as data and never as SQL.

**Q: How are passwords stored?**
As **BCrypt hashes**, using `BCryptPasswordEncoder`. A hash can't be turned back into the password, and each hash has its own random salt. The MPIN is hashed the same way.

**Q: How does login work?**
The login API checks the password, creates a random UUID **token**, and keeps `token → userId` in a `ConcurrentHashMap`. The browser saves the token in `localStorage` and sends it in the `Authorization: Bearer` header. `AuthInterceptor` checks it before every API call and blocks `/api/admin/**` for non-admins.
*Improvement:* use JWT or Spring Security, and store sessions in Redis so logins survive a server restart.

**Q: What if the server crashes in the middle of a transfer?**
`autoCommit` is false, so nothing is saved until `commit()`. The database throws away the unfinished transaction, so money is never debited without also being credited.

**Q: What if two transfers run at the same time?**
`SELECT … FOR UPDATE` locks the rows. Locking in id order prevents deadlocks.

**Q: Why BigDecimal for money?**
`double` has rounding errors (0.1 + 0.2 = 0.30000000000000004). `BigDecimal` is exact.

**Q: What is Dependency Injection?**
Spring creates the objects and passes them into constructors. My classes don't create their own dependencies, which makes them loosely coupled and easy to test.

**Q: What does `@RestControllerAdvice` do?**
It is one global error handler. A service just throws `BankException("Insufficient balance")`, and the handler turns it into an HTTP 400 JSON response. No try/catch is needed in the controllers.

**Q: Difference between `executeQuery` and `executeUpdate`?**
`executeQuery` is for SELECT and returns a `ResultSet`. `executeUpdate` is for INSERT, UPDATE, and DELETE, and returns the number of rows changed.

**Q: What is a connection pool?**
Opening a DB connection is slow, so a pool (HikariCP) keeps some connections open and reuses them. `con.close()` gives the connection back to the pool.

**Q: How would you scale this?**
Use MySQL or PostgreSQL, JWT authentication, Redis for sessions, pagination on statements, Docker, and separate microservices (accounts, payments, loans) talking through a message queue.

---

## 12. Switch to MySQL (optional)
In `application.properties`, comment out the H2 lines and uncomment the MySQL lines, then set your password. Nothing else changes, because JDBC code is the same for every database.

## 13. Run with Docker (optional)

```bash
docker build -t bank-management .
docker run -p 8080:8080 bank-management
```
Then open http://localhost:8080.
