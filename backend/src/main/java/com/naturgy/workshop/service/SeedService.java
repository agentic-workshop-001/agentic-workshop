package com.naturgy.workshop.service;

import com.naturgy.workshop.entity.*;
import com.naturgy.workshop.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class SeedService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    private final MeterRepository meterRepo;
    private final ContractRepository contractRepo;
    private final ReadingRepository readingRepo;

    public SeedService(MeterRepository meterRepo, ContractRepository contractRepo, ReadingRepository readingRepo) {
        this.meterRepo = meterRepo;
        this.contractRepo = contractRepo;
        this.readingRepo = readingRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedMeters();
        seedContracts();
        seedReadings();
    }

    private void seedMeters() throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource("seed/meters.csv").getInputStream()))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.isBlank()) continue;
                String[] f = line.split(",", -1);
                String meterId = f[0].trim();
                if (meterRepo.existsById(meterId)) continue;
                Meter m = new Meter();
                m.setMeterId(meterId);
                m.setCups(blank(f[1]));
                m.setAddress(f[2].trim());
                m.setPostalCode(blank(f[3]));
                m.setCity(f[4].trim());
                meterRepo.save(m);
            }
        }
        log.info("Meters seeded");
    }

    private void seedContracts() throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource("seed/contracts.csv").getInputStream()))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.isBlank()) continue;
                String[] f = line.split(",", -1);
                String contractId = f[0].trim();
                if (contractRepo.existsById(contractId)) continue;
                Contract c = new Contract();
                c.setContractId(contractId);
                c.setMeterId(f[1].trim());
                c.setCustomerId(f[2].trim());
                c.setFullName(f[3].trim());
                c.setNif(f[4].trim());
                c.setEmail(blank(f[5]));
                c.setContractType(ContractType.valueOf(f[6].trim()));
                c.setStartDate(LocalDate.parse(f[7].trim()));
                c.setEndDate(blank(f[8]) == null ? null : LocalDate.parse(f[8].trim()));
                c.setBillingCycle(BillingCycle.valueOf(f[9].trim()));
                c.setFlatMonthlyFeeEur(decimal(f[10]));
                c.setIncludedKwh(decimal(f[11]));
                c.setOveragePricePerKwhEur(decimal(f[12]));
                c.setFixedPricePerKwhEur(decimal(f[13]));
                c.setTaxRate(new BigDecimal(f[14].trim()));
                c.setIban(blank(f[15]));
                contractRepo.save(c);
            }
        }
        log.info("Contracts seeded");
    }

    private void seedReadings() throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource("seed/readings.csv").getInputStream()))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.isBlank()) continue;
                String[] f = line.split(",", -1);
                String meterId = f[0].trim();
                LocalDate date = LocalDate.parse(f[1].trim());
                Integer hour = Integer.parseInt(f[2].trim());
                ReadingId rid = new ReadingId(meterId, date, hour);
                if (readingRepo.existsById(rid)) {
                    log.debug("Duplicate reading skipped: {} {} {}", meterId, date, hour);
                    continue;
                }
                Reading r = new Reading();
                r.setMeterId(meterId);
                r.setDate(date);
                r.setHour(hour);
                r.setKwh(new BigDecimal(f[3].trim()));
                String qual = blank(f[4]);
                r.setQuality(qual == null ? null : Quality.valueOf(qual));
                readingRepo.save(r);
            }
        }
        log.info("Readings seeded");
    }

    private String blank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private BigDecimal decimal(String s) {
        String t = blank(s);
        return t == null ? null : new BigDecimal(t);
    }
}
