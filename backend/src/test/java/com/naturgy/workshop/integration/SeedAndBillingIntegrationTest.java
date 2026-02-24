package com.naturgy.workshop.integration;

import com.naturgy.workshop.entity.Invoice;
import com.naturgy.workshop.repository.ContractRepository;
import com.naturgy.workshop.repository.InvoiceRepository;
import com.naturgy.workshop.repository.MeterRepository;
import com.naturgy.workshop.repository.ReadingRepository;
import com.naturgy.workshop.service.BillingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SeedAndBillingIntegrationTest {

    @Autowired
    private MeterRepository meterRepo;

    @Autowired
    private ContractRepository contractRepo;

    @Autowired
    private ReadingRepository readingRepo;

    @Autowired
    private InvoiceRepository invoiceRepo;

    @Autowired
    private BillingService billingService;

    @Test
    void seedDataIsLoaded() {
        assertTrue(meterRepo.count() >= 2, "Expected at least 2 meters");
        assertTrue(contractRepo.count() >= 2, "Expected at least 2 contracts");
        assertTrue(readingRepo.count() >= 5, "Expected at least 5 readings");
    }

    @Test
    void billingRun_createsInvoices_and_pdfDownloadable() {
        List<Invoice> invoices = billingService.runBilling("2026-01");

        assertNotNull(invoices);
        assertFalse(invoices.isEmpty(), "Expected at least one invoice");

        for (Invoice inv : invoices) {
            assertNotNull(inv.getInvoiceId());
            assertEquals("2026-01", inv.getPeriod());
            assertNotNull(inv.getSubtotal());
            assertNotNull(inv.getTax());
            assertNotNull(inv.getTotal());

            // PDF should be downloadable
            assertNotNull(inv.getPdfPath(), "PDF path should be set");
            byte[] bytes = billingService.getPdfBytes(inv.getInvoiceId());
            assertNotNull(bytes);
            assertTrue(bytes.length > 0, "PDF should have content");
        }

        // Invoices persisted
        List<Invoice> persisted = invoiceRepo.findByPeriod("2026-01");
        assertFalse(persisted.isEmpty());
    }

    @Test
    void billingRun_fixedContractCalculation() {
        List<Invoice> invoices = billingService.runBilling("2026-01");
        // CONT001 is FIXED for MTR0001
        Invoice fixed = invoices.stream()
                .filter(i -> i.getContractId().equals("CONT001"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("CONT001 invoice not found"));

        // MTR0001 readings: 0.45 + 0.40 + 0.38 = 1.23 kWh
        // subtotal = 1.23 * 0.19 = 0.23
        assertEquals(0, fixed.getSubtotal().compareTo(new java.math.BigDecimal("0.23")));
    }
}
