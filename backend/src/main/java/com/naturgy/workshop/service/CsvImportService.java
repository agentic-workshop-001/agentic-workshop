package com.naturgy.workshop.service;

import com.naturgy.workshop.domain.enums.BillingCycle;
import com.naturgy.workshop.domain.enums.ContractType;
import com.naturgy.workshop.domain.enums.ReadingQuality;
import com.naturgy.workshop.domain.model.Contract;
import com.naturgy.workshop.domain.model.Meter;
import com.naturgy.workshop.domain.model.Reading;
import com.naturgy.workshop.domain.model.ReadingId;
import com.naturgy.workshop.domain.repository.ContractRepository;
import com.naturgy.workshop.domain.repository.MeterRepository;
import com.naturgy.workshop.domain.repository.ReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles multipart CSV import for meters, contracts, and readings.
 * Validation errors are collected and returned; duplicate rows are skipped.
 */
@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    private final MeterRepository    meterRepo;
    private final ContractRepository contractRepo;
    private final ReadingRepository  readingRepo;

    public CsvImportService(MeterRepository meterRepo,
                            ContractRepository contractRepo,
                            ReadingRepository readingRepo) {
        this.meterRepo    = meterRepo;
        this.contractRepo = contractRepo;
        this.readingRepo  = readingRepo;
    }

    public record ImportResult(int inserted, int skipped, List<String> errors) {}

    @Transactional
    public ImportResult importMeters(MultipartFile file) throws Exception {
        int inserted = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        for (String[] row : readCsv(file)) {
            String meterId    = col(row, 0);
            String cups       = col(row, 1);
            String address    = col(row, 2);
            String postalCode = col(row, 3);
            String city       = col(row, 4);

            if (meterId == null || meterId.isBlank()) {
                errors.add("Row skipped: meterId is required – " + rowStr(row));
                skipped++;
                continue;
            }
            if (address == null || address.isBlank()) {
                errors.add("Row skipped: address is required for meterId=" + meterId);
                skipped++;
                continue;
            }
            if (city == null || city.isBlank()) {
                errors.add("Row skipped: city is required for meterId=" + meterId);
                skipped++;
                continue;
            }
            if (meterRepo.existsById(meterId)) {
                log.debug("[Import] Meter already exists, skipping: {}", meterId);
                skipped++;
                continue;
            }
            meterRepo.save(new Meter(meterId, cups, address, postalCode, city));
            inserted++;
        }
        return new ImportResult(inserted, skipped, errors);
    }

    @Transactional
    public ImportResult importContracts(MultipartFile file) throws Exception {
        int inserted = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        for (String[] row : readCsv(file)) {
            String contractId = col(row, 0);
            String meterId    = col(row, 1);

            if (contractId == null || contractId.isBlank()) {
                errors.add("Row skipped: contractId is required – " + rowStr(row));
                skipped++;
                continue;
            }
            if (contractRepo.existsById(contractId)) {
                log.debug("[Import] Contract already exists, skipping: {}", contractId);
                skipped++;
                continue;
            }
            Meter meter = meterRepo.findById(meterId).orElse(null);
            if (meter == null) {
                errors.add("Row skipped: unknown meterId='" + meterId + "' for contract=" + contractId);
                skipped++;
                continue;
            }
            try {
                String       customerId = col(row, 2);
                String       fullName   = col(row, 3);
                String       nif        = col(row, 4);
                String       email      = col(row, 5);
                ContractType type       = ContractType.valueOf(col(row, 6));
                LocalDate    startDate  = LocalDate.parse(col(row, 7));
                LocalDate    endDate    = parseDate(col(row, 8));
                BillingCycle cycle      = BillingCycle.valueOf(col(row, 9));
                BigDecimal   flatFee    = parseDecimal(col(row, 10));
                BigDecimal   inclKwh    = parseDecimal(col(row, 11));
                BigDecimal   overage    = parseDecimal(col(row, 12));
                BigDecimal   fixedPrice = parseDecimal(col(row, 13));
                BigDecimal   taxRate    = new BigDecimal(col(row, 14));
                String       iban       = col(row, 15);

                contractRepo.save(new Contract(contractId, meter, customerId, fullName, nif, email,
                        type, startDate, endDate, cycle, flatFee, inclKwh, overage, fixedPrice, taxRate, iban));
                inserted++;
            } catch (Exception e) {
                errors.add("Row error for contract=" + contractId + ": " + e.getMessage());
                skipped++;
            }
        }
        return new ImportResult(inserted, skipped, errors);
    }

    @Transactional
    public ImportResult importReadings(MultipartFile file) throws Exception {
        int inserted = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        for (String[] row : readCsv(file)) {
            String meterId = col(row, 0);
            try {
                LocalDate    date    = LocalDate.parse(col(row, 1));
                Integer      hour    = Integer.parseInt(col(row, 2));
                BigDecimal   kwh     = new BigDecimal(col(row, 3));
                String       qualStr = col(row, 4);
                ReadingQuality quality = (qualStr != null && !qualStr.isBlank())
                        ? ReadingQuality.valueOf(qualStr) : null;

                if (hour < 0 || hour > 23) {
                    errors.add("Row skipped: hour must be 0-23 for meterId=" + meterId + " date=" + date);
                    skipped++;
                    continue;
                }
                if (kwh.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Row skipped: kwh must be >= 0 for meterId=" + meterId + " date=" + date);
                    skipped++;
                    continue;
                }

                ReadingId rid = new ReadingId(meterId, date, hour);
                if (readingRepo.existsById(rid)) {
                    errors.add("Row skipped: duplicate reading meterId=" + meterId + " date=" + date + " hour=" + hour);
                    skipped++;
                    continue;
                }

                Meter meter = meterRepo.findById(meterId).orElse(null);
                if (meter == null) {
                    errors.add("Row skipped: unknown meterId='" + meterId + "'");
                    skipped++;
                    continue;
                }

                readingRepo.save(new Reading(rid, meter, kwh, quality));
                inserted++;
            } catch (Exception e) {
                errors.add("Row error for meterId=" + meterId + ": " + e.getMessage());
                skipped++;
            }
        }
        return new ImportResult(inserted, skipped, errors);
    }

    // ── CSV utilities ─────────────────────────────────────────────────────────

    private List<String[]> readCsv(MultipartFile file) throws Exception {
        try (var reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)
                    .filter(l -> !l.isBlank())
                    .map(l -> l.split(",", -1))
                    .toList();
        }
    }

    private String col(String[] row, int index) {
        if (index >= row.length) return null;
        String v = row[index].trim();
        return v.isEmpty() ? null : v;
    }

    private LocalDate parseDate(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
    }

    private BigDecimal parseDecimal(String s) {
        return (s == null || s.isBlank()) ? null : new BigDecimal(s);
    }

    private String rowStr(String[] row) {
        return String.join(",", row);
    }
}
