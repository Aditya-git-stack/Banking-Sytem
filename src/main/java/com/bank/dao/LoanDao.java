package com.bank.dao;

import com.bank.model.Loan;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the 'loans' table. */
@Repository
public class LoanDao {

    public void insert(Connection con, Loan l) throws SQLException {
        String sql = "INSERT INTO loans (account_id, loan_type, principal, interest_rate, tenure_months, emi, outstanding, emis_paid, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, l.getAccountId());
            ps.setString(2, l.getLoanType());
            ps.setBigDecimal(3, l.getPrincipal());
            ps.setBigDecimal(4, l.getInterestRate());
            ps.setInt(5, l.getTenureMonths());
            ps.setBigDecimal(6, l.getEmi());
            ps.setBigDecimal(7, l.getOutstanding());
            ps.setString(8, l.getStatus());
            ps.executeUpdate();
        }
    }

    public List<Loan> findByAccountId(Connection con, long accountId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM loans WHERE account_id = ? ORDER BY id DESC")) {
            ps.setLong(1, accountId);
            return readAll(ps);
        }
    }

    public List<Loan> findAll(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM loans ORDER BY CASE WHEN status = 'PENDING' THEN 0 ELSE 1 END, id DESC")) {
            return readAll(ps);
        }
    }

    public Loan findById(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM loans WHERE id = ?")) {
            ps.setLong(1, id);
            List<Loan> list = readAll(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    public void updateStatus(Connection con, long id, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE loans SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void updateRepayment(Connection con, long id, BigDecimal outstanding, int emisPaid, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE loans SET outstanding = ?, emis_paid = ?, status = ? WHERE id = ?")) {
            ps.setBigDecimal(1, outstanding);
            ps.setInt(2, emisPaid);
            ps.setString(3, status);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    public int countByStatus(Connection con, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM loans WHERE status = ?")) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<Loan> readAll(PreparedStatement ps) throws SQLException {
        List<Loan> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Loan l = new Loan();
                l.setId(rs.getLong("id"));
                l.setAccountId(rs.getLong("account_id"));
                l.setLoanType(rs.getString("loan_type"));
                l.setPrincipal(rs.getBigDecimal("principal"));
                l.setInterestRate(rs.getBigDecimal("interest_rate"));
                l.setTenureMonths(rs.getInt("tenure_months"));
                l.setEmi(rs.getBigDecimal("emi"));
                l.setOutstanding(rs.getBigDecimal("outstanding"));
                l.setEmisPaid(rs.getInt("emis_paid"));
                l.setStatus(rs.getString("status"));
                l.setAppliedAt(rs.getTimestamp("applied_at").toLocalDateTime());
                list.add(l);
            }
        }
        return list;
    }
}
