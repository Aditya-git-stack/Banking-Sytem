package com.bank.controller;

import com.bank.dto.Requests;
import com.bank.model.Beneficiary;
import com.bank.service.BeneficiaryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping
    public List<Beneficiary> list(@RequestAttribute("userId") long userId) throws SQLException {
        return beneficiaryService.list(userId);
    }

    @PostMapping
    public Map<String, String> add(@RequestAttribute("userId") long userId, @Valid @RequestBody Requests.AddBeneficiary request) throws SQLException {
        beneficiaryService.add(userId, request);
        return Map.of("message", "Beneficiary added");
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@RequestAttribute("userId") long userId, @PathVariable long id) throws SQLException {
        beneficiaryService.delete(userId, id);
        return Map.of("message", "Beneficiary removed");
    }
}
