package com.bank.service;

import com.bank.dao.AccountDao;
import com.bank.dao.UserDao;
import com.bank.dto.Requests;
import com.bank.exception.BankException;
import com.bank.model.Account;
import com.bank.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registration, login and logout.
 * Login creates a random token (UUID) and remembers "token -> user" in memory.
 * The browser sends this token with every request; AuthInterceptor checks it.
 */
@Service
public class AuthService {

    /** Who is logged in with a given token. */
    public record Session(long userId, String role) {
    }

    public static final String IFSC = "SMBK0001234";
    public static final String BRANCH = "Bhubaneswar Main Branch";
    public static final String UPI_SUFFIX = "@bank";

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    private final DataSource dataSource;
    private final UserDao userDao;
    private final AccountDao accountDao;

    // Spring automatically passes these objects in (Dependency Injection)
    public AuthService(DataSource dataSource, UserDao userDao, AccountDao accountDao) {
        this.dataSource = dataSource;
        this.userDao = userDao;
        this.accountDao = accountDao;
    }

    /** Creates the user AND their bank account together: both succeed or both are rolled back. */
    public long register(Requests.Register r) throws SQLException {
        String accountType = r.accountType().toUpperCase();
        if (!accountType.equals("SAVINGS") && !accountType.equals("CURRENT")) {
            throw new BankException("Account type must be SAVINGS or CURRENT");
        }
        try (Connection con = dataSource.getConnection()) {
            if (userDao.exists(con, "username", r.username())) throw new BankException("Username already taken");
            if (userDao.exists(con, "email", r.email())) throw new BankException("Email already registered");
            if (userDao.exists(con, "mobile", r.mobile())) throw new BankException("Mobile number already registered");

            con.setAutoCommit(false);           // start a DB transaction
            try {
                User user = new User();
                user.setFullName(r.fullName());
                user.setEmail(r.email());
                user.setMobile(r.mobile());
                user.setUsername(r.username());
                user.setPasswordHash(encoder.encode(r.password()));
                user.setMpinHash(encoder.encode(r.mpin()));
                user.setRole("CUSTOMER");
                long userId = userDao.insert(con, user);

                Account account = new Account();
                account.setUserId(userId);
                account.setAccountNumber(generateUniqueAccountNumber(con));
                account.setAccountType(accountType);
                account.setBalance(BigDecimal.ZERO);
                account.setStatus("ACTIVE");
                account.setIfsc(IFSC);
                account.setBranch(BRANCH);
                account.setUpiId(r.mobile() + UPI_SUFFIX);
                account.setCardNumber("4" + randomDigits(15));
                accountDao.insert(con, account);

                con.commit();                   // save both rows
                return userId;
            } catch (Exception e) {
                con.rollback();                 // undo everything if anything failed
                throw e;
            }
        }
    }

    public void createAdmin(String name, String email, String mobile, String username, String password) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            User admin = new User();
            admin.setFullName(name);
            admin.setEmail(email);
            admin.setMobile(mobile);
            admin.setUsername(username);
            admin.setPasswordHash(encoder.encode(password));
            admin.setMpinHash(encoder.encode("0000"));
            admin.setRole("ADMIN");
            userDao.insert(con, admin);
        }
    }

    public Map<String, Object> login(Requests.Login r) throws SQLException {
        User user;
        try (Connection con = dataSource.getConnection()) {
            user = userDao.findByUsername(con, r.username());
        }
        if (user == null || !encoder.matches(r.password(), user.getPasswordHash())) {
            throw new BankException("Invalid username or password");
        }
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(user.getId(), user.getRole()));
        return Map.of("token", token, "role", user.getRole(), "fullName", user.getFullName());
    }

    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    public Session getSession(String token) {
        return token == null ? null : sessions.get(token);
    }

    public boolean usernameExists(String username) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            return userDao.exists(con, "username", username);
        }
    }

    public String hash(String raw) {
        return encoder.encode(raw);
    }

    public boolean matches(String raw, String hash) {
        return encoder.matches(raw, hash);
    }

    private String generateUniqueAccountNumber(Connection con) throws SQLException {
        String number;
        do {
            number = "3" + randomDigits(10);  // 11 digits like SBI
        } while (accountDao.findByAccountNumber(con, number) != null);
        return number;
    }

    private String randomDigits(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }
}
