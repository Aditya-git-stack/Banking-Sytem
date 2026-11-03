package com.bank.dao;

import com.bank.model.Account;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the 'accounts' table. */
@Repository
public class AccountDao {

    public long insert(Connection con, Account a) throws SQLException {
        String sql = "INSERT INTO accounts (user_id, account_number, account_type, balance, status, ifsc, branch, upi_id, card_number, card_blocked) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, a.getUserId());
            ps.setString(2, a.getAccountNumber());
            ps.setString(3, a.getAccountType());
            ps.setBigDecimal(4, a.getBalance());
            ps.setString(5, a.getStatus());
            ps.setString(6, a.getIfsc());
            ps.setString(7, a.getBranch());
            ps.setString(8, a.getUpiId());
            ps.setString(9, a.getCardNumber());
            ps.setBoolean(10, a.isCardBlocked());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public Account findById(Connection con, long id) throws SQLException {
        return findOne(con, "SELECT * FROM accounts WHERE id = ?", id);
    }

    public Account findByUserId(Connection con, long userId) throws SQLException {
        return findOne(con, "SELECT * FROM accounts WHERE user_id = ?", userId);
    }

    public Account findByAccountNumber(Connection con, String accountNumber) throws SQLException {
        return findOne(con, "SELECT * FROM accounts WHERE account_number = ?", accountNumber);
    }

    public Account findByUpiId(Connection con, String upiId) throws SQLException {
        return findOne(con, "SELECT * FROM accounts WHERE upi_id = ?", upiId);
    }

    /**
     * SELECT ... FOR UPDATE locks the row until commit/rollback, so two transfers
     * running at the same moment cannot both read the old balance (no double spending).
     */
    public Account findByIdForUpdate(Connection con, long id) throws SQLException {
        return findOne(con, "SELECT * FROM accounts WHERE id = ? FOR UPDATE", id);
    }

    public List<Account> findAll(Connection con) throws SQLException {
        List<Account> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void updateBalance(Connection con, long id, BigDecimal newBalance) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?")) {
            ps.setBigDecimal(1, newBalance);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection con, long id, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void updateCardBlocked(Connection con, long id, boolean blocked) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET card_blocked = ? WHERE id = ?")) {
            ps.setBoolean(1, blocked);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public BigDecimal totalBalance(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(balance), 0) FROM accounts");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }

    private Account findOne(Connection con, String sql, Object param) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getLong("id"));
        a.setUserId(rs.getLong("user_id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setAccountType(rs.getString("account_type"));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setStatus(rs.getString("status"));
        a.setIfsc(rs.getString("ifsc"));
        a.setBranch(rs.getString("branch"));
        a.setUpiId(rs.getString("upi_id"));
        a.setCardNumber(rs.getString("card_number"));
        a.setCardBlocked(rs.getBoolean("card_blocked"));
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return a;
    }
}
