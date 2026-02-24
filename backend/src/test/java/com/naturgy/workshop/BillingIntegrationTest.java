package com.naturgy.workshop;

import com.naturgy.workshop.domain.model.Invoice;
import com.naturgy.workshop.domain.repository.InvoiceRepository;
import com.naturgy.workshop.service.BillingService;
import com.naturgy.workshop.service.PdfService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: seed data → billing run → invoice list → PDF download.
 *
 * <p>Uses the standard H2 in-memory DB with seed data from resources/db/samples/*.csv.
 */
@SpringBootTest
class BillingIntegrationTest {

    @Autowired BillingService    billingService;
    @Autowired InvoiceRepository invoiceRepo;
    @Autowired PdfService        pdfService;

    @Test
    @DisplayName("Integration: seed → billing 2026-01 → 2 invoices → PDF bytes")
    void billingRunAndPdf() throws Exception {
        // Run billing for period where seeded readings exist (2026-01)
        List<Invoice> generated = billingService.runBilling("2026-01");

        // Both seeded contracts (CONT001/FIXED and CONT002/FLAT) should be billed
        assertThat(generated).hasSizeGreaterThanOrEqualTo(2);

        // Verify invoices are persisted
        List<Invoice> all = invoiceRepo.findByPeriod("2026-01");
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);

        // Verify FIXED invoice (CONT001 / MTR0001)
        Invoice fixedInv = all.stream()
                .filter(i -> "CONT001".equals(i.getContractId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No invoice for CONT001"));

        // MTR0001 readings 2026-01-01: 0.45 + 0.40 + 0.38 = 1.23 kWh
        // subtotal = 1.23 * 0.19 = 0.23; tax = 0.23 * 0.21 = 0.05; total = 0.28
        assertThat(fixedInv.getTotalKwh()).isEqualByComparingTo("1.230");
        assertThat(fixedInv.getSubtotal()).isEqualByComparingTo("0.23");

        // Verify FLAT invoice (CONT002 / MTR0002)
        Invoice flatInv = all.stream()
                .filter(i -> "CONT002".equals(i.getContractId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No invoice for CONT002"));

        // MTR0002 readings 2026-01-01: 0.60 + 0.55 = 1.15 kWh (< 200 includedKwh)
        // subtotal = 45.00 (no overage); tax = 45.00 * 0.21 = 9.45; total = 54.45
        assertThat(flatInv.getTotalKwh()).isEqualByComparingTo("1.150");
        assertThat(flatInv.getSubtotal()).isEqualByComparingTo("45.00");
        assertThat(flatInv.getTax()).isEqualByComparingTo("9.45");
        assertThat(flatInv.getTotal()).isEqualByComparingTo("54.45");

        // PDF generation
        byte[] pdf = pdfService.generateInvoicePdf(fixedInv);
        assertThat(pdf).isNotEmpty();
        // PDF magic bytes: %PDF
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

        // Running billing again for same period → idempotent (no new invoices)
        List<Invoice> secondRun = billingService.runBilling("2026-01");
        assertThat(secondRun).isEmpty();
    }
}
