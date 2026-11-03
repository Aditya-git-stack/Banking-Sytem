package com.bank.controller;

import com.bank.dto.Receipt;
import com.bank.dto.Requests;
import com.bank.model.FixedDeposit;
import com.bank.service.FixedDepositService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fd")
public class FixedDepositController {

    private final FixedDepositService fdService;

    public FixedDepositController(FixedDepositService fdService) {
        this.fdService = fdService;
    }

    @GetMapping
    public List<FixedDeposit> list(@RequestAttribute("userId") long userId) throws SQLException {
        return fdService.list(userId);
    }

    @GetMapping("/preview")
    public Map<String, Object> preview(@RequestParam BigDecimal amount, @RequestParam int months) {
        return fdService.preview(amount, months);
    }

    @PostMapping
    public Receipt open(@RequestAttribute("userId") long userId, @Valid @RequestBody Requests.OpenFd request) throws SQLException {
        return fdService.open(userId, request);
    }

    @PostMapping("/{id}/close")
    public Receipt close(@RequestAttribute("userId") long userId, @PathVariable long id,
                         @Valid @RequestBody Requests.Mpin request) throws SQLException {
        return fdService.close(userId, id, request.mpin());
    }
}
