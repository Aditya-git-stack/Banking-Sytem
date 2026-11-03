package com.bank.controller;

import com.bank.dto.Receipt;
import com.bank.dto.Requests;
import com.bank.service.BillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping("/pay")
    public Receipt pay(@RequestAttribute("userId") long userId, @Valid @RequestBody Requests.PayBill request) throws SQLException {
        return billService.pay(userId, request);
    }
}
