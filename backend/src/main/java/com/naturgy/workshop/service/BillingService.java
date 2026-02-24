package com.naturgy.workshop.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.naturgy.workshop.entity.Contract;
import com.naturgy.workshop.entity.ContractType;
import com.naturgy.workshop.entity.Invoice;
import com.naturgy.workshop.exception.ResourceNotFoundException;
import com.naturgy.workshop.exception.ValidationException;
import com.naturgy.workshop.repository.ContractRepository;
import com.naturgy.workshop.repository.InvoiceRepository;
import com.naturgy.workshop.repository.ReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final ContractRepository contractRepo;
    private final ReadingRepository readingRepo;
    private final InvoiceRepository invoiceRepo;

    public BillingService(ContractRepository contractRepo,
                          ReadingRepository readingRepo,
                          InvoiceRepository invoiceRepo) {
        this.contractRepo = contractRepo;
        this.readingRepo = readingRepo;
        this.invoiceRepo = invoiceRepo;
    }

    @Transactional
    public List<Invoice> runBilling(String period) {
        YearMonth ym = parsePeriod(period);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Contract> contracts = contractRepo.findAll();
        List<Invoice> result = new ArrayList<>();

        for (Contract contract : contracts) {
            BigDecimal totalKwh = readingRepo.sumKwhByMeterIdAndDateBetween(
                    contract.getMeterId(), start, end);

            Invoice invoice = buildInvoice(contract, period, totalKwh);
            invoice = invoiceRepo.save(invoice);
            generatePdf(invoice, contract);
            invoice = invoiceRepo.save(invoice);
            result.add(invoice);
        }
        return result;
    }

    public Invoice buildInvoice(Contract contract, String period, BigDecimal totalKwh) {
        BigDecimal subtotal;

        if (contract.getContractType() == ContractType.FIXED) {
            if (contract.getFixedPricePerKwhEur() == null) {
                throw new ValidationException("FIXED contract missing fixedPricePerKwhEur: " + contract.getContractId());
            }
            subtotal = totalKwh.multiply(contract.getFixedPricePerKwhEur())
                    .setScale(MONEY_SCALE, ROUNDING);
        } else {
            // FLAT
            if (contract.getFlatMonthlyFeeEur() == null || contract.getIncludedKwh() == null
                    || contract.getOveragePricePerKwhEur() == null) {
                throw new ValidationException("FLAT contract missing required fields: " + contract.getContractId());
            }
            BigDecimal overageKwh = totalKwh.subtract(contract.getIncludedKwh())
                    .max(BigDecimal.ZERO);
            BigDecimal overage = overageKwh.multiply(contract.getOveragePricePerKwhEur())
                    .setScale(MONEY_SCALE, ROUNDING);
            subtotal = contract.getFlatMonthlyFeeEur().add(overage)
                    .setScale(MONEY_SCALE, ROUNDING);
        }

        BigDecimal tax = subtotal.multiply(contract.getTaxRate())
                .setScale(MONEY_SCALE, ROUNDING);
        BigDecimal total = subtotal.add(tax).setScale(MONEY_SCALE, ROUNDING);

        Invoice invoice = new Invoice();
        invoice.setContractId(contract.getContractId());
        invoice.setMeterId(contract.getMeterId());
        invoice.setPeriod(period);
        invoice.setTotalKwh(totalKwh.setScale(3, ROUNDING));
        invoice.setSubtotal(subtotal);
        invoice.setTax(tax);
        invoice.setTotal(total);
        return invoice;
    }

    private void generatePdf(Invoice invoice, Contract contract) {
        try {
            Path pdfDir = Path.of(System.getProperty("java.io.tmpdir"), "invoices");
            Files.createDirectories(pdfDir);
            File pdfFile = pdfDir.resolve(invoice.getInvoiceId() + ".pdf").toFile();

            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(pdfFile));
            doc.open();

            doc.add(new Paragraph("INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Invoice ID: " + invoice.getInvoiceId()));
            doc.add(new Paragraph("Period: " + invoice.getPeriod()));
            doc.add(new Paragraph("Contract: " + invoice.getContractId()));
            doc.add(new Paragraph("Customer: " + contract.getFullName()));
            doc.add(new Paragraph("NIF: " + contract.getNif()));
            doc.add(new Paragraph("Meter: " + invoice.getMeterId()));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Total kWh: " + invoice.getTotalKwh()));
            doc.add(new Paragraph("Subtotal: " + invoice.getSubtotal() + " EUR"));
            doc.add(new Paragraph("Tax (" + contract.getTaxRate().multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%): " + invoice.getTax() + " EUR"));
            doc.add(new Paragraph("TOTAL: " + invoice.getTotal() + " EUR"));
            doc.close();

            invoice.setPdfPath(pdfFile.getAbsolutePath());
        } catch (Exception e) {
            // PDF generation failure is non-fatal; invoice is still saved
        }
    }

    public byte[] getPdfBytes(String invoiceId) {
        Invoice invoice = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        if (invoice.getPdfPath() == null) {
            throw new ResourceNotFoundException("PDF not available for invoice: " + invoiceId);
        }
        try {
            return Files.readAllBytes(Path.of(invoice.getPdfPath()));
        } catch (Exception e) {
            throw new ResourceNotFoundException("PDF file not found: " + invoice.getPdfPath());
        }
    }

    private YearMonth parsePeriod(String period) {
        try {
            return YearMonth.parse(period);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid period format. Expected YYYY-MM, got: " + period);
        }
    }
}
