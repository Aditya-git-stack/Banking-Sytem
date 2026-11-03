-- All tables of the bank. Runs automatically when the app starts.

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    mobile        VARCHAR(15)  NOT NULL UNIQUE,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,   -- BCrypt hash, never the real password
    mpin_hash     VARCHAR(100) NOT NULL,   -- 4-digit transaction PIN, also hashed
    role          VARCHAR(20)  NOT NULL,   -- CUSTOMER or ADMIN
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounts (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    account_number   VARCHAR(20) NOT NULL UNIQUE,
    account_type     VARCHAR(20) NOT NULL,          -- SAVINGS or CURRENT
    balance          DECIMAL(15,2) NOT NULL DEFAULT 0,
    status           VARCHAR(20) NOT NULL,          -- ACTIVE or FROZEN
    ifsc             VARCHAR(15) NOT NULL,
    branch           VARCHAR(100) NOT NULL,
    upi_id           VARCHAR(50) NOT NULL UNIQUE,
    card_number      VARCHAR(20) NOT NULL,
    card_blocked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id     BIGINT NOT NULL,
    type           VARCHAR(10) NOT NULL,            -- CREDIT (money in) or DEBIT (money out)
    category       VARCHAR(20) NOT NULL,            -- DEPOSIT, WITHDRAW, TRANSFER, BILL, RECHARGE, FD, LOAN
    amount         DECIMAL(15,2) NOT NULL,
    balance_after  DECIMAL(15,2) NOT NULL,
    description    VARCHAR(255),
    reference_no   VARCHAR(30) NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE IF NOT EXISTS beneficiaries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    account_number  VARCHAR(20) NOT NULL,
    ifsc            VARCHAR(15) NOT NULL,
    nickname        VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS fixed_deposits (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id       BIGINT NOT NULL,
    principal        DECIMAL(15,2) NOT NULL,
    interest_rate    DECIMAL(5,2) NOT NULL,
    tenure_months    INT NOT NULL,
    maturity_amount  DECIMAL(15,2) NOT NULL,
    start_date       DATE NOT NULL,
    maturity_date    DATE NOT NULL,
    status           VARCHAR(20) NOT NULL,           -- ACTIVE or CLOSED
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE IF NOT EXISTS loans (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id     BIGINT NOT NULL,
    loan_type      VARCHAR(20) NOT NULL,             -- HOME, CAR, PERSONAL, EDUCATION
    principal      DECIMAL(15,2) NOT NULL,
    interest_rate  DECIMAL(5,2) NOT NULL,
    tenure_months  INT NOT NULL,
    emi            DECIMAL(15,2) NOT NULL,
    outstanding    DECIMAL(15,2) NOT NULL,
    emis_paid      INT NOT NULL DEFAULT 0,
    status         VARCHAR(20) NOT NULL,             -- PENDING, APPROVED, REJECTED, CLOSED
    applied_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);
