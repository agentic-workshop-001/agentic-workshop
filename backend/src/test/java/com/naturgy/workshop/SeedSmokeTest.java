package com.naturgy.workshop;

import com.naturgy.workshop.domain.enums.ContractType;
import com.naturgy.workshop.domain.enums.ReadingQuality;
import com.naturgy.workshop.domain.model.ReadingId;
import com.naturgy.workshop.domain.repository.ContractRepository;
import com.naturgy.workshop.domain.repository.MeterRepository;
import com.naturgy.workshop.domain.repository.ReadingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boot + seed smoke test.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Application context loads without errors</li>
 *   <li>Seed CSVs were imported with correct row counts</li>
 *   <li>Entity relationships are navigable</li>
 *   <li>Seeder is idempotent (running it twice does not duplicate rows)</li>
 *   <li>ReadingRepository aggregation query returns correct kWh sum</li>
 * </ul>
 */
@SpringBootTest
class SeedSmokeTest {

    @Autowired MeterRepository    meterRepo;
    @Autowired ContractRepository contractRepo;
    @Autowired ReadingRepository  readingRepo;
    @Autowired com.naturgy.workshop.seed.DatabaseSeeder seeder;

    // ── Row-count assertions ───────────────────────────────────────────────────

    @Test
    @DisplayName("meters.csv: 2 meters seeded")
    void metersSeeded() {
        assertThat(meterRepo.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("contracts.csv: 2 contracts seeded")
    void contractsSeeded() {
        assertThat(contractRepo.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("readings.csv: 5 readings seeded")
    void readingsSeeded() {
        assertThat(readingRepo.count()).isEqualTo(5);
    }

    // ── Entity correctness ────────────────────────────────────────────────────

    @Test
    @DisplayName("MTR0001 has correct city and CUPS")
    void meterMTR0001() {
        var m = meterRepo.findById("MTR0001").orElseThrow();
        assertThat(m.getCity()).isEqualTo("Valencia");
        assertThat(m.getCups()).isEqualTo("ES0021000000000001");
        assertThat(m.getAddress()).isEqualTo("C/ Mayor 10");
        assertThat(m.getPostalCode()).isEqualTo("46001");
    }

    @Test
    @DisplayName("CONT001 is FIXED type with correct tax rate")
    void contractCONT001() {
        var c = contractRepo.findById("CONT001").orElseThrow();
        assertThat(c.getContractType()).isEqualTo(ContractType.FIXED);
        assertThat(c.getFixedPricePerKwhEur()).isEqualByComparingTo(new BigDecimal("0.19"));
        assertThat(c.getTaxRate()).isEqualByComparingTo(new BigDecimal("0.21"));
        assertThat(c.getFlatMonthlyFeeEur()).isNull();
        assertThat(c.getEndDate()).isNull(); // open-ended
    }

    @Test
    @DisplayName("CONT002 is FLAT type with correct overage price")
    void contractCONT002() {
        var c = contractRepo.findById("CONT002").orElseThrow();
        assertThat(c.getContractType()).isEqualTo(ContractType.FLAT);
        assertThat(c.getFlatMonthlyFeeEur()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(c.getIncludedKwh()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(c.getOveragePricePerKwhEur()).isEqualByComparingTo(new BigDecimal("0.28"));
        assertThat(c.getFixedPricePerKwhEur()).isNull();
    }

    @Test
    @DisplayName("Reading MTR0001@2026-01-01h0 has correct kwh and quality")
    void readingMTR0001h0() {
        var rid = new ReadingId("MTR0001", LocalDate.of(2026, 1, 1), 0);
        var r   = readingRepo.findById(rid).orElseThrow();
        assertThat(r.getKwh()).isEqualByComparingTo(new BigDecimal("0.45"));
        assertThat(r.getQuality()).isEqualTo(ReadingQuality.REAL);
    }

    @Test
    @DisplayName("Reading MTR0002@2026-01-01h0 has quality ESTIMATED")
    void readingMTR0002h0_estimated() {
        var rid = new ReadingId("MTR0002", LocalDate.of(2026, 1, 1), 0);
        var r   = readingRepo.findById(rid).orElseThrow();
        assertThat(r.getQuality()).isEqualTo(ReadingQuality.ESTIMATED);
    }

    // ── Relationship navigation ───────────────────────────────────────────────

    @Test
    @DisplayName("CONT001 FK → MTR0001 navigable")
    void contractFkNavigable() {
        var c = contractRepo.findById("CONT001").orElseThrow();
        assertThat(c.getMeter().getMeterId()).isEqualTo("MTR0001");
    }

    @Test
    @DisplayName("ContractRepository: find by meterId")
    void contractsByMeterId() {
        var list = contractRepo.findByMeter_MeterId("MTR0001");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getContractId()).isEqualTo("CONT001");
    }

    @Test
    @DisplayName("ReadingRepository: findByIdMeterIdAndIdDateBetween for MTR0001")
    void readingsByMeterAndDateRange() {
        var list = readingRepo.findByIdMeterIdAndIdDateBetween(
                "MTR0001",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1));
        assertThat(list).hasSize(3); // hours 0, 1, 2
    }

    @Test
    @DisplayName("ReadingRepository: sumKwh for MTR0001 on 2026-01-01 = 1.23")
    void sumKwhMTR0001() {
        BigDecimal sum = readingRepo.sumKwhByMeterIdAndDateBetween(
                "MTR0001",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1));
        // 0.45 + 0.40 + 0.38 = 1.23
        assertThat(sum).isEqualByComparingTo(new BigDecimal("1.23"));
    }

    // ── Idempotency ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Running seeder twice does not duplicate rows")
    void seederIsIdempotent() throws Exception {
        long metersBefore    = meterRepo.count();
        long contractsBefore = contractRepo.count();
        long readingsBefore  = readingRepo.count();

        // Second run – ApplicationRunner invoked manually
        seeder.run(null);

        assertThat(meterRepo.count()).isEqualTo(metersBefore);
        assertThat(contractRepo.count()).isEqualTo(contractsBefore);
        assertThat(readingRepo.count()).isEqualTo(readingsBefore);
    }
}
