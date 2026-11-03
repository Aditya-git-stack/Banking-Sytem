package com.bank.dao;

import com.bank.model.FixedDeposit;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the 'fixed_deposits' table. */
@Repository
public class FixedDepositDao {

    public void insert(Connection con, FixedDeposit fd) throws SQLException {
        String sql = "INSERT INTO fixed_deposits (account_id, principal, interest_rate, tenure_months, maturity_amount, start_date, maturity_date, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, fd.getAccountId());
            ps.setBigDecimal(2, fd.getPrincipal());
            ps.setBigDecimal(3, fd.getInterestRate());
            ps.setInt(4, fd.getTenureMonths());
            ps.setBigDecimal(5, fd.getMaturityAmount());
            ps.setDate(6, Date.valueOf(fd.getStartDate()));
            ps.setDate(7, Date.valueOf(fd.getMaturityDate()));
            ps.setString(8, fd.getStatus());
            ps.executeUpdate();
        }
    }

    public List<FixedDeposit> findByAccountId(Connection con, long accountId) throws SQLException {
        List<FixedDeposit> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM fixed_deposits WHERE account_id = ? ORDER BY id DESC")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public FixedDeposit findById(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM fixed_deposits WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void updateStatus(Connection con, long id, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE fixed_deposits SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private FixedDeposit mapRow(ResultSet rs) throws SQLException {
        FixedDeposit fd = new FixedDeposit();
        fd.setId(rs.getLong("id"));
        fd.setAccountId(rs.getLong("account_id"));
        fd.setPrincipal(rs.getBigDecimal("principal"));
        fd.setInterestRate(rs.getBigDecimal("interest_rate"));
        fd.setTenureMonths(rs.getInt("tenure_months"));
        fd.setMaturityAmount(rs.getBigDecimal("maturity_amount"));
        fd.setStartDate(rs.getDate("start_date").toLocalDate());
        fd.setMaturityDate(rs.getDate("maturity_date").toLocalDate());
        fd.setStatus(rs.getString("status"));
        return fd;
    }
}
