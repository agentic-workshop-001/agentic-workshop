package com.naturgy.workshop.controller;

import com.naturgy.workshop.entity.Invoice;
import com.naturgy.workshop.service.BillingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/run")
    public List<Invoice> run(@RequestParam String period) {
        return billingService.runBilling(period);
    }
}
