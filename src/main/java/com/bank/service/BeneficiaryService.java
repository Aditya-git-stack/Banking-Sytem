package com.bank.service;

import com.bank.dao.AccountDao;
import com.bank.dao.BeneficiaryDao;
import com.bank.dto.Requests;
import com.bank.exception.BankException;
import com.bank.model.Account;
import com.bank.model.Beneficiary;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Add / list / delete saved payees. */
@Service
public class BeneficiaryService {

    private final DataSource dataSource;
    private final BeneficiaryDao beneficiaryDao;
    private final AccountDao accountDao;

    public BeneficiaryService(DataSource dataSource, BeneficiaryDao beneficiaryDao, AccountDao accountDao) {
        this.dataSource = dataSource;
        this.beneficiaryDao = beneficiaryDao;
        this.accountDao = accountDao;
    }

    public List<Beneficiary> list(long userId) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            return beneficiaryDao.findByUserId(con, userId);
        }
    }

    public void add(long userId, Requests.AddBeneficiary r) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            Account target = accountDao.findByAccountNumber(con, r.accountNumber());
            if (target == null) throw new BankException("Account number does not exist in our bank");
            if (target.getUserId() == userId) throw new BankException("You cannot add your own account");
            if (beneficiaryDao.exists(con, userId, r.accountNumber())) throw new BankException("Beneficiary already added");

            Beneficiary b = new Beneficiary();
            b.setUserId(userId);
            b.setName(r.name());
            b.setAccountNumber(r.accountNumber());
            b.setIfsc(r.ifsc().toUpperCase());
            b.setNickname(r.nickname());
            beneficiaryDao.insert(con, b);
        }
    }

    public void delete(long userId, long beneficiaryId) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            if (beneficiaryDao.delete(con, beneficiaryId, userId) == 0) throw new BankException("Beneficiary not found");
        }
    }
}
