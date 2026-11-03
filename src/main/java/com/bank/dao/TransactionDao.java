package com.bank.dao;

import com.bank.model.Transaction;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the 'transactions' table (the passbook / statement). */
@Repository
public class TransactionDao {

    public void insert(Connection con, Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions (account_id, type, category, amount, balance_after, description, reference_no) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, t.getAccountId());
            ps.setString(2, t.getType());
            ps.setString(3, t.getCategory());
            ps.setBigDecimal(4, t.getAmount());
            ps.setBigDecimal(5, t.getBalanceAfter());
            ps.setString(6, t.getDescription());
            ps.setString(7, t.getReferenceNo());
            ps.executeUpdate();
        }
    }

    /** Latest N transactions of one account (mini statement). */
    public List<Transaction> findRecent(Connection con, long accountId, int limit) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setInt(2, limit);
            return readAll(ps);
        }
    }

    /** Full statement between two dates (both inclusive). */
    public List<Transaction> findBetween(Connection con, long accountId, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE account_id = ? AND created_at >= ? AND created_at < ? ORDER BY id DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setTimestamp(2, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(3, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            return readAll(ps);
        }
    }

    /** Latest N transactions of the whole bank (admin view). */
    public List<Transaction> findAllRecent(Connection con, int limit) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM transactions ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            return readAll(ps);
        }
    }

    /** Total money sent by transfer today, used for the daily transfer limit. */
    public BigDecimal sumTransfersToday(Connection con, long accountId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions "
                   + "WHERE account_id = ? AND type = 'DEBIT' AND category = 'TRANSFER' AND created_at >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDate.now().atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    public int count(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM transactions");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private List<Transaction> readAll(PreparedStatement ps) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Transaction t = new Transaction();
                t.setId(rs.getLong("id"));
                t.setAccountId(rs.getLong("account_id"));
                t.setType(rs.getString("type"));
                t.setCategory(rs.getString("category"));
                t.setAmount(rs.getBigDecimal("amount"));
                t.setBalanceAfter(rs.getBigDecimal("balance_after"));
                t.setDescription(rs.getString("description"));
                t.setReferenceNo(rs.getString("reference_no"));
                t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(t);
            }
        }
        return list;
    }
}
