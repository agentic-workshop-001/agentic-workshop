package com.naturgy.workshop.service;

import com.naturgy.workshop.entity.Contract;
import com.naturgy.workshop.entity.ContractType;
import com.naturgy.workshop.entity.Invoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BillingServiceTest {

    private BillingService billingService;

    @BeforeEach
    void setUp() {
        billingService = new BillingService(mock(com.naturgy.workshop.repository.ContractRepository.class),
                mock(com.naturgy.workshop.repository.ReadingRepository.class),
                mock(com.naturgy.workshop.repository.InvoiceRepository.class));
    }

    private Contract fixedContract() {
        Contract c = new Contract();
        c.setContractId("TEST-FIXED");
        c.setMeterId("MTR0001");
        c.setContractType(ContractType.FIXED);
        c.setFixedPricePerKwhEur(new BigDecimal("0.19"));
        c.setTaxRate(new BigDecimal("0.21"));
        c.setStartDate(LocalDate.of(2025, 1, 1));
        return c;
    }

    private Contract flatContract() {
        Contract c = new Contract();
        c.setContractId("TEST-FLAT");
        c.setMeterId("MTR0002");
        c.setContractType(ContractType.FLAT);
        c.setFlatMonthlyFeeEur(new BigDecimal("45.00"));
        c.setIncludedKwh(new BigDecimal("200"));
        c.setOveragePricePerKwhEur(new BigDecimal("0.28"));
        c.setTaxRate(new BigDecimal("0.21"));
        c.setStartDate(LocalDate.of(2025, 6, 1));
        return c;
    }

    @Test
    void fixedContract_noOverage_calculatesCorrectly() {
        Contract c = fixedContract();
        BigDecimal totalKwh = new BigDecimal("1.23");

        Invoice inv = billingService.buildInvoice(c, "2026-01", totalKwh);

        // subtotal = 1.23 * 0.19 = 0.2337 → 0.23
        assertEquals(new BigDecimal("0.23"), inv.getSubtotal());
        // tax = 0.23 * 0.21 = 0.0483 → 0.05
        assertEquals(new BigDecimal("0.05"), inv.getTax());
        // total = 0.23 + 0.05 = 0.28
        assertEquals(new BigDecimal("0.28"), inv.getTotal());
        assertEquals(new BigDecimal("1.230"), inv.getTotalKwh());
    }

    @Test
    void fixedContract_zero_kwh() {
        Contract c = fixedContract();
        Invoice inv = billingService.buildInvoice(c, "2026-01", BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), inv.getSubtotal());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), inv.getTotal());
    }

    @Test
    void flatContract_noOverage() {
        Contract c = flatContract();
        BigDecimal totalKwh = new BigDecimal("150");

        Invoice inv = billingService.buildInvoice(c, "2026-01", totalKwh);

        // overageKwh = max(0, 150 - 200) = 0
        // subtotal = 45.00 + 0 = 45.00
        assertEquals(new BigDecimal("45.00"), inv.getSubtotal());
        // tax = 45.00 * 0.21 = 9.45
        assertEquals(new BigDecimal("9.45"), inv.getTax());
        // total = 54.45
        assertEquals(new BigDecimal("54.45"), inv.getTotal());
    }

    @Test
    void flatContract_withOverage() {
        Contract c = flatContract();
        BigDecimal totalKwh = new BigDecimal("250");

        Invoice inv = billingService.buildInvoice(c, "2026-01", totalKwh);

        // overageKwh = 250 - 200 = 50
        // overage = 50 * 0.28 = 14.00
        // subtotal = 45.00 + 14.00 = 59.00
        assertEquals(new BigDecimal("59.00"), inv.getSubtotal());
        // tax = 59.00 * 0.21 = 12.39
        assertEquals(new BigDecimal("12.39"), inv.getTax());
        // total = 71.39
        assertEquals(new BigDecimal("71.39"), inv.getTotal());
    }

    @Test
    void flatContract_seedData_noOverage() {
        // MTR0002 seed readings: 0.60 + 0.55 = 1.15 kWh for 2026-01
        Contract c = flatContract();
        BigDecimal totalKwh = new BigDecimal("1.15");
        Invoice inv = billingService.buildInvoice(c, "2026-01", totalKwh);
        // overageKwh = max(0, 1.15 - 200) = 0
        assertEquals(new BigDecimal("45.00"), inv.getSubtotal());
        assertEquals(new BigDecimal("9.45"), inv.getTax());
        assertEquals(new BigDecimal("54.45"), inv.getTotal());
    }
}
