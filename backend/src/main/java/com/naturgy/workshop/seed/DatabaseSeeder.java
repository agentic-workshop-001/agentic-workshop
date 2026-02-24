package com.naturgy.workshop.seed;

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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Idempotent startup seeder.
 *
 * <p>Import order: meters → contracts → readings (FK dependency order).
 *
 * <p>Idempotency strategy (clarifications.txt):
 * <ul>
 *   <li>Meters:    skip if meterId already exists</li>
 *   <li>Contracts: skip if contractId already exists</li>
 *   <li>Readings:  skip silently (debug log) if (meterId+date+hour) already exists</li>
 * </ul>
 *
 * <p>CSV format: comma-separated, first row is header, empty fields → null.
 */
@Component
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private static final String METERS_CSV    = "db/samples/meters.csv";
    private static final String CONTRACTS_CSV = "db/samples/contracts.csv";
    private static final String READINGS_CSV  = "db/samples/readings.csv";

    private final MeterRepository    meterRepo;
    private final ContractRepository contractRepo;
    private final ReadingRepository  readingRepo;

    public DatabaseSeeder(MeterRepository meterRepo,
                          ContractRepository contractRepo,
                          ReadingRepository readingRepo) {
        this.meterRepo    = meterRepo;
        this.contractRepo = contractRepo;
        this.readingRepo  = readingRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== DatabaseSeeder: starting seed import ===");
        importMeters();
        importContracts();
        importReadings();
        log.info("=== DatabaseSeeder: seed import complete – meters={}, contracts={}, readings={} ===",
                meterRepo.count(), contractRepo.count(), readingRepo.count());
    }

    // ── Meters ────────────────────────────────────────────────────────────────

    private void importMeters() throws Exception {
        List<String[]> rows = readCsv(METERS_CSV);
        int inserted = 0, skipped = 0;

        for (String[] row : rows) {
            // columns: meterId, cups, address, postalCode, city
            String meterId    = col(row, 0);
            String cups       = col(row, 1);
            String address    = col(row, 2);
            String postalCode = col(row, 3);
            String city       = col(row, 4);

            if (meterId == null || meterId.isBlank()) {
                log.warn("[Meters] Skipping row with empty meterId: {}", rowStr(row));
                skipped++;
                continue;
            }

            if (meterRepo.existsById(meterId)) {
                log.debug("[Meters] Already exists, skipping: {}", meterId);
                skipped++;
                continue;
            }

            meterRepo.save(new Meter(meterId, cups, address, postalCode, city));
            log.debug("[Meters] Inserted: {}", meterId);
            inserted++;
        }
        log.info("[Meters] imported={} skipped={}", inserted, skipped);
    }

    // ── Contracts ─────────────────────────────────────────────────────────────

    private void importContracts() throws Exception {
        List<String[]> rows = readCsv(CONTRACTS_CSV);
        int inserted = 0, skipped = 0;

        for (String[] row : rows) {
            // contractId,meterId,customerId,fullName,nif,email,contractType,
            // startDate,endDate,billingCycle,flatMonthlyFeeEur,includedKwh,
            // overagePricePerKwhEur,fixedPricePerKwhEur,taxRate,iban
            String contractId  = col(row, 0);
            String meterId     = col(row, 1);

            if (contractId == null || contractId.isBlank()) {
                log.warn("[Contracts] Skipping row with empty contractId: {}", rowStr(row));
                skipped++;
                continue;
            }

            if (contractRepo.existsById(contractId)) {
                log.debug("[Contracts] Already exists, skipping: {}", contractId);
                skipped++;
                continue;
            }

            Meter meter = meterRepo.findById(meterId).orElseThrow(() ->
                    new IllegalStateException("[Contracts] Unknown meterId '" + meterId
                            + "' referenced by contract '" + contractId + "'"));

            String       customerId   = col(row, 2);
            String       fullName     = col(row, 3);
            String       nif          = col(row, 4);
            String       email        = col(row, 5);
            ContractType type         = ContractType.valueOf(col(row, 6));
            LocalDate    startDate    = LocalDate.parse(col(row, 7));
            LocalDate    endDate      = parseDate(col(row, 8));
            BillingCycle cycle        = BillingCycle.valueOf(col(row, 9));
            BigDecimal   flatFee      = parseDecimal(col(row, 10));
            BigDecimal   includedKwh  = parseDecimal(col(row, 11));
            BigDecimal   overage      = parseDecimal(col(row, 12));
            BigDecimal   fixedPrice   = parseDecimal(col(row, 13));
            BigDecimal   taxRate      = new BigDecimal(col(row, 14));
            String       iban         = col(row, 15);

            contractRepo.save(new Contract(
                    contractId, meter, customerId, fullName, nif, email,
                    type, startDate, endDate, cycle,
                    flatFee, includedKwh, overage, fixedPrice, taxRate, iban));
            log.debug("[Contracts] Inserted: {}", contractId);
            inserted++;
        }
        log.info("[Contracts] imported={} skipped={}", inserted, skipped);
    }

    // ── Readings ──────────────────────────────────────────────────────────────

    private void importReadings() throws Exception {
        List<String[]> rows = readCsv(READINGS_CSV);
        int inserted = 0, skipped = 0;

        for (String[] row : rows) {
            // meterId,date,hour,kwh,quality
            String    meterId = col(row, 0);
            LocalDate date    = LocalDate.parse(col(row, 1));
            Integer   hour    = Integer.parseInt(col(row, 2));
            BigDecimal kwh    = new BigDecimal(col(row, 3));
            String    qualStr = col(row, 4);
            ReadingQuality quality = (qualStr != null && !qualStr.isBlank())
                    ? ReadingQuality.valueOf(qualStr)
                    : null;

            ReadingId rid = new ReadingId(meterId, date, hour);

            if (readingRepo.existsById(rid)) {
                log.debug("[Readings] Duplicate, skipping: {}", rid);
                skipped++;
                continue;
            }

            Meter meter = meterRepo.findById(meterId).orElseThrow(() ->
                    new IllegalStateException("[Readings] Unknown meterId '" + meterId + "'"));

            readingRepo.save(new Reading(rid, meter, kwh, quality));
            log.debug("[Readings] Inserted: {}", rid);
            inserted++;
        }
        log.info("[Readings] imported={} skipped={}", inserted, skipped);
    }

    // ── CSV utilities ─────────────────────────────────────────────────────────

    /**
     * Reads a classpath CSV, skips the header row, returns each data row as
     * a raw String[] split by comma.
     */
    private List<String[]> readCsv(String classpathLocation) throws Exception {
        var resource = new ClassPathResource(classpathLocation);
        try (var reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)                        // skip header
                    .filter(l -> !l.isBlank())
                    .map(l -> l.split(",", -1))     // -1 preserves trailing empty fields
                    .toList();
        }
    }

    /** Returns trimmed column value, or null when empty. */
    private String col(String[] row, int index) {
        if (index >= row.length) return null;
        String v = row[index].trim();
        return v.isEmpty() ? null : v;
    }

    /** Parses a date string; returns null for null/empty input. */
    private LocalDate parseDate(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
    }

    /** Parses a BigDecimal; returns null for null/empty input. */
    private BigDecimal parseDecimal(String s) {
        return (s == null || s.isBlank()) ? null : new BigDecimal(s);
    }

    private String rowStr(String[] row) {
        return String.join(",", row);
    }
}
