package com.bank.dao;

import com.bank.model.Beneficiary;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the 'beneficiaries' table (saved payees). */
@Repository
public class BeneficiaryDao {

    public void insert(Connection con, Beneficiary b) throws SQLException {
        String sql = "INSERT INTO beneficiaries (user_id, name, account_number, ifsc, nickname) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, b.getUserId());
            ps.setString(2, b.getName());
            ps.setString(3, b.getAccountNumber());
            ps.setString(4, b.getIfsc());
            ps.setString(5, b.getNickname());
            ps.executeUpdate();
        }
    }

    public List<Beneficiary> findByUserId(Connection con, long userId) throws SQLException {
        List<Beneficiary> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM beneficiaries WHERE user_id = ? ORDER BY name")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Beneficiary b = new Beneficiary();
                    b.setId(rs.getLong("id"));
                    b.setUserId(rs.getLong("user_id"));
                    b.setName(rs.getString("name"));
                    b.setAccountNumber(rs.getString("account_number"));
                    b.setIfsc(rs.getString("ifsc"));
                    b.setNickname(rs.getString("nickname"));
                    list.add(b);
                }
            }
        }
        return list;
    }

    public boolean exists(Connection con, long userId, String accountNumber) throws SQLException {
        String sql = "SELECT COUNT(*) FROM beneficiaries WHERE user_id = ? AND account_number = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** user_id in the WHERE makes sure a user can only delete their own beneficiary. */
    public int delete(Connection con, long id, long userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM beneficiaries WHERE id = ? AND user_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate();
        }
    }
}
