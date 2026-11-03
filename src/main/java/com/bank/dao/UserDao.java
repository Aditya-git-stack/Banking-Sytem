package com.bank.dao;

import com.bank.model.User;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO = Data Access Object. The ONLY place that writes SQL for the 'users' table.
 * Every method receives a JDBC Connection so the caller (service) can decide
 * whether several queries must run inside one database transaction.
 */
@Repository
public class UserDao {

    public long insert(Connection con, User u) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, mobile, username, password_hash, mpin_hash, role) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getMobile());
            ps.setString(4, u.getUsername());
            ps.setString(5, u.getPasswordHash());
            ps.setString(6, u.getMpinHash());
            ps.setString(7, u.getRole());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);   // the new auto-generated id
            }
        }
    }

    public User findById(Connection con, long id) throws SQLException {
        return findOne(con, "SELECT * FROM users WHERE id = ?", id);
    }

    public User findByUsername(Connection con, String username) throws SQLException {
        return findOne(con, "SELECT * FROM users WHERE username = ?", username);
    }

    /** column is one of our own fixed names (username/email/mobile), never user input. */
    public boolean exists(Connection con, String column, String value) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM users WHERE " + column + " = ?")) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public List<User> findAllCustomers(Connection con) throws SQLException {
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE role = 'CUSTOMER' ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void updatePassword(Connection con, long userId, String hash) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
            ps.setString(1, hash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateMpin(Connection con, long userId, String hash) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE users SET mpin_hash = ? WHERE id = ?")) {
            ps.setString(1, hash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    private User findOne(Connection con, String sql, Object param) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Converts the current ResultSet row into a User object. */
    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setMobile(rs.getString("mobile"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setMpinHash(rs.getString("mpin_hash"));
        u.setRole(rs.getString("role"));
        u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return u;
    }
}
