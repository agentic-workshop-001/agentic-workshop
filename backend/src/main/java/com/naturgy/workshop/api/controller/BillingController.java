package com.naturgy.workshop.api.controller;

import com.naturgy.workshop.domain.model.Invoice;
import com.naturgy.workshop.domain.repository.InvoiceRepository;
import com.naturgy.workshop.service.BillingService;
import com.naturgy.workshop.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class BillingController {

    private final BillingService    billingService;
    private final InvoiceRepository invoiceRepo;
    private final PdfService        pdfService;

    public BillingController(BillingService billingService,
                             InvoiceRepository invoiceRepo,
                             PdfService pdfService) {
        this.billingService = billingService;
        this.invoiceRepo    = invoiceRepo;
        this.pdfService     = pdfService;
    }

    /**
     * Execute billing for a given period (YYYY-MM).
     * POST /api/billing/run?period=2026-01
     */
    @PostMapping("/billing/run")
    public ResponseEntity<Map<String, Object>> runBilling(@RequestParam String period) {
        List<Invoice> generated = billingService.runBilling(period);
        return ResponseEntity.ok(Map.of(
                "period", period,
                "generated", generated.size(),
                "invoices", generated.stream().map(Invoice::getInvoiceId).toList()
        ));
    }

    /**
     * List all invoices, optionally filtered by period.
     * GET /api/invoices
     * GET /api/invoices?period=2026-01
     */
    @GetMapping("/invoices")
    public List<Invoice> listInvoices(@RequestParam(required = false) String period) {
        if (period != null) {
            return invoiceRepo.findByPeriod(period);
        }
        return invoiceRepo.findAllByOrderByPeriodDescGeneratedAtDesc();
    }

    /**
     * Get a single invoice by ID.
     * GET /api/invoices/{id}
     */
    @GetMapping("/invoices/{id}")
    public Invoice getInvoice(@PathVariable String id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + id));
    }

    /**
     * Download invoice as PDF.
     * GET /api/invoices/{id}/pdf
     */
    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) throws Exception {
        Invoice invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + id));
        byte[] pdf = pdfService.generateInvoicePdf(invoice);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invoice-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
