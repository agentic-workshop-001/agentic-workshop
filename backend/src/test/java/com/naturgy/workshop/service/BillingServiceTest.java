package com.naturgy.workshop.service;

import com.naturgy.workshop.domain.enums.BillingCycle;
import com.naturgy.workshop.domain.enums.ContractType;
import com.naturgy.workshop.domain.model.Contract;
import com.naturgy.workshop.domain.model.Invoice;
import com.naturgy.workshop.domain.model.Meter;
import com.naturgy.workshop.domain.repository.ContractRepository;
import com.naturgy.workshop.domain.repository.InvoiceRepository;
import com.naturgy.workshop.domain.repository.ReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BillingService FIXED/FLAT calculation logic.
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock ContractRepository contractRepo;
    @Mock ReadingRepository  readingRepo;
    @Mock InvoiceRepository  invoiceRepo;

    @InjectMocks BillingService billingService;

    private Meter mtr0001;
    private Meter mtr0002;

    @BeforeEach
    void setUp() {
        mtr0001 = new Meter("MTR0001", "ES0021000000000001", "C/ Mayor 10", "46001", "Valencia");
        mtr0002 = new Meter("MTR0002", "ES0021000000000002", "Av. Aragón 55", "46021", "Valencia");
    }

    // ── calculateFixed ────────────────────────────────────────────────────────

    @Test
    @DisplayName("FIXED: energy = totalKwh * fixedPrice; tax and total correct")
    void fixedCalculation() {
        // 100 kWh * 0.19 EUR/kWh = 19.00 subtotal; 19.00 * 0.21 = 3.99 tax; total = 22.99
        BigDecimal subtotal = billingService.calculateFixed(
                new BigDecimal("100.00"),
                new BigDecimal("0.19"));
        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("19.00"));
    }

    @Test
    @DisplayName("FIXED: rounding HALF_UP (e.g. 3 kWh * 0.19 = 0.57)")
    void fixedRounding() {
        BigDecimal subtotal = billingService.calculateFixed(
                new BigDecimal("3"),
                new BigDecimal("0.19"));
        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("0.57"));
    }

    @Test
    @DisplayName("FIXED: throws if fixedPricePerKwhEur is null")
    void fixedNullPrice() {
        assertThatThrownBy(() -> billingService.calculateFixed(new BigDecimal("100"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── calculateFlat ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("FLAT: no overage (totalKwh <= includedKwh) → subtotal = flatFee")
    void flatNoOverage() {
        // 150 kWh consumed, 200 kWh included → no overage
        BigDecimal subtotal = billingService.calculateFlat(
                new BigDecimal("150"),
                new BigDecimal("45.00"),
                new BigDecimal("200"),
                new BigDecimal("0.28"));
        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("45.00"));
    }

    @Test
    @DisplayName("FLAT: overage (totalKwh > includedKwh) → subtotal = flatFee + overage")
    void flatWithOverage() {
        // 250 kWh consumed, 200 kWh included → 50 kWh overage * 0.28 = 14.00
        // subtotal = 45.00 + 14.00 = 59.00
        BigDecimal subtotal = billingService.calculateFlat(
                new BigDecimal("250"),
                new BigDecimal("45.00"),
                new BigDecimal("200"),
                new BigDecimal("0.28"));
        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("59.00"));
    }

    @Test
    @DisplayName("FLAT: zero consumption → subtotal = flatFee")
    void flatZeroConsumption() {
        BigDecimal subtotal = billingService.calculateFlat(
                BigDecimal.ZERO,
                new BigDecimal("45.00"),
                new BigDecimal("200"),
                new BigDecimal("0.28"));
        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("45.00"));
    }

    @Test
    @DisplayName("FLAT: throws if required fields are null")
    void flatNullFields() {
        assertThatThrownBy(() -> billingService.calculateFlat(
                new BigDecimal("100"), null, new BigDecimal("200"), new BigDecimal("0.28")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── generateInvoice end-to-end ────────────────────────────────────────────

    @Test
    @DisplayName("generateInvoice FIXED: correct total with 21% tax")
    void generateInvoiceFixed() {
        Contract contract = new Contract(
                "CONT001", mtr0001, "CUST001", "Ana Perez", "12345678Z", "ana@test.com",
                ContractType.FIXED, LocalDate.of(2025, 1, 1), null, BillingCycle.MONTHLY,
                null, null, null, new BigDecimal("0.19"), new BigDecimal("0.21"), null);

        when(readingRepo.sumKwhByMeterIdAndDateBetween(eq("MTR0001"), any(), any()))
                .thenReturn(new BigDecimal("100.00"));

        Invoice invoice = billingService.generateInvoice(contract, "2026-01",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        // subtotal = 100 * 0.19 = 19.00; tax = 19.00 * 0.21 = 3.99; total = 22.99
        assertThat(invoice.getSubtotal()).isEqualByComparingTo(new BigDecimal("19.00"));
        assertThat(invoice.getTax()).isEqualByComparingTo(new BigDecimal("3.99"));
        assertThat(invoice.getTotal()).isEqualByComparingTo(new BigDecimal("22.99"));
        assertThat(invoice.getContractType()).isEqualTo(ContractType.FIXED);
        assertThat(invoice.getPeriod()).isEqualTo("2026-01");
    }

    @Test
    @DisplayName("generateInvoice FLAT with overage: correct total with 21% tax")
    void generateInvoiceFlatWithOverage() {
        Contract contract = new Contract(
                "CONT002", mtr0002, "CUST002", "Roberto Garcia", "87654321X", "rob@test.com",
                ContractType.FLAT, LocalDate.of(2025, 6, 1), null, BillingCycle.MONTHLY,
                new BigDecimal("45.00"), new BigDecimal("200"), new BigDecimal("0.28"),
                null, new BigDecimal("0.21"), null);

        when(readingRepo.sumKwhByMeterIdAndDateBetween(eq("MTR0002"), any(), any()))
                .thenReturn(new BigDecimal("250.00"));

        Invoice invoice = billingService.generateInvoice(contract, "2026-01",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        // subtotal = 45.00 + (50 * 0.28) = 45.00 + 14.00 = 59.00
        // tax = 59.00 * 0.21 = 12.39; total = 71.39
        assertThat(invoice.getSubtotal()).isEqualByComparingTo(new BigDecimal("59.00"));
        assertThat(invoice.getTax()).isEqualByComparingTo(new BigDecimal("12.39"));
        assertThat(invoice.getTotal()).isEqualByComparingTo(new BigDecimal("71.39"));
        assertThat(invoice.getContractType()).isEqualTo(ContractType.FLAT);
    }

    // ── runBilling ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("runBilling: invalid period format throws IllegalArgumentException")
    void runBillingInvalidPeriod() {
        assertThatThrownBy(() -> billingService.runBilling("2026/01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid period format");
    }

    @Test
    @DisplayName("runBilling: skips contract if invoice already exists")
    void runBillingSkipExisting() {
        Contract contract = new Contract(
                "CONT001", mtr0001, "CUST001", "Ana", "12345678Z", null,
                ContractType.FIXED, LocalDate.of(2025, 1, 1), null, BillingCycle.MONTHLY,
                null, null, null, new BigDecimal("0.19"), new BigDecimal("0.21"), null);

        when(contractRepo.findAll()).thenReturn(List.of(contract));
        when(invoiceRepo.findByContractIdAndPeriod("CONT001", "2026-01"))
                .thenReturn(Optional.of(mock(Invoice.class)));

        List<Invoice> result = billingService.runBilling("2026-01");
        assertThat(result).isEmpty();
        verify(invoiceRepo, never()).save(any());
    }

    @Test
    @DisplayName("runBilling: generates invoice for active contract")
    void runBillingGeneratesInvoice() {
        Contract contract = new Contract(
                "CONT001", mtr0001, "CUST001", "Ana", "12345678Z", null,
                ContractType.FIXED, LocalDate.of(2025, 1, 1), null, BillingCycle.MONTHLY,
                null, null, null, new BigDecimal("0.19"), new BigDecimal("0.21"), null);

        when(contractRepo.findAll()).thenReturn(List.of(contract));
        when(invoiceRepo.findByContractIdAndPeriod("CONT001", "2026-01")).thenReturn(Optional.empty());
        when(readingRepo.sumKwhByMeterIdAndDateBetween(eq("MTR0001"), any(), any()))
                .thenReturn(new BigDecimal("100.00"));
        when(invoiceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Invoice> result = billingService.runBilling("2026-01");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotal()).isEqualByComparingTo(new BigDecimal("22.99"));
    }

    @Test
    @DisplayName("runBilling: contract with endDate before period is excluded")
    void runBillingExcludesExpiredContract() {
        Contract contract = new Contract(
                "CONT001", mtr0001, "CUST001", "Ana", "12345678Z", null,
                ContractType.FIXED, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BillingCycle.MONTHLY, null, null, null, new BigDecimal("0.19"),
                new BigDecimal("0.21"), null);

        when(contractRepo.findAll()).thenReturn(List.of(contract));

        List<Invoice> result = billingService.runBilling("2026-01");
        assertThat(result).isEmpty();
    }
}
