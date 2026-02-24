package com.naturgy.workshop.controller;

import com.naturgy.workshop.entity.Invoice;
import com.naturgy.workshop.repository.InvoiceRepository;
import com.naturgy.workshop.service.BillingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepo;
    private final BillingService billingService;

    public InvoiceController(InvoiceRepository invoiceRepo, BillingService billingService) {
        this.invoiceRepo = invoiceRepo;
        this.billingService = billingService;
    }

    @GetMapping
    public List<Invoice> getAll(@RequestParam(required = false) String period) {
        if (period != null) {
            return invoiceRepo.findByPeriod(period);
        }
        return invoiceRepo.findAll();
    }

    @GetMapping("/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String invoiceId) {
        byte[] bytes = billingService.getPdfBytes(invoiceId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + invoiceId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
