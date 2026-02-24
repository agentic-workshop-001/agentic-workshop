package com.naturgy.workshop.service;

import com.naturgy.workshop.domain.enums.ContractType;
import com.naturgy.workshop.domain.model.Contract;
import com.naturgy.workshop.domain.model.Invoice;
import com.naturgy.workshop.domain.repository.ContractRepository;
import com.naturgy.workshop.domain.repository.InvoiceRepository;
import com.naturgy.workshop.domain.repository.ReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Billing service: executes FIXED/FLAT invoice calculations per logic-spec.
 *
 * <p>FIXED: energy = totalKwh * fixedPricePerKwhEur; subtotal = energy
 * <p>FLAT:  base = flatMonthlyFeeEur; overageKwh = max(0, totalKwh - includedKwh);
 *           overage = overageKwh * overagePricePerKwhEur; subtotal = base + overage
 * <p>Both: tax = subtotal * taxRate; total = subtotal + tax
 * <p>Rounding: 2 decimals, HALF_UP
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final ContractRepository contractRepo;
    private final ReadingRepository  readingRepo;
    private final InvoiceRepository  invoiceRepo;

    public BillingService(ContractRepository contractRepo,
                          ReadingRepository readingRepo,
                          InvoiceRepository invoiceRepo) {
        this.contractRepo = contractRepo;
        this.readingRepo  = readingRepo;
        this.invoiceRepo  = invoiceRepo;
    }

    /**
     * Run billing for all active contracts in the given period (YYYY-MM).
     * If an invoice already exists for a contract+period it is skipped.
     *
     * @param period YYYY-MM string
     * @return list of generated invoices
     */
    @Transactional
    public List<Invoice> runBilling(String period) {
        YearMonth ym = parseYearMonth(period);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();

        List<Contract> activeContracts = findActiveContracts(from, to);
        log.info("[Billing] period={} active contracts={}", period, activeContracts.size());

        List<Invoice> generated = new ArrayList<>();
        for (Contract contract : activeContracts) {
            if (invoiceRepo.findByContractIdAndPeriod(contract.getContractId(), period).isPresent()) {
                log.debug("[Billing] Invoice already exists for contract={} period={}", contract.getContractId(), period);
                continue;
            }
            Invoice invoice = generateInvoice(contract, period, from, to);
            invoiceRepo.save(invoice);
            generated.add(invoice);
            log.info("[Billing] Generated invoice={} contract={} total={}", invoice.getInvoiceId(), contract.getContractId(), invoice.getTotal());
        }
        return generated;
    }

    /**
     * Calculate invoice for a single contract and period (not persisted – used for unit tests).
     */
    public Invoice generateInvoice(Contract contract, String period, LocalDate from, LocalDate to) {
        String meterId = contract.getMeter().getMeterId();
        BigDecimal totalKwh = readingRepo.sumKwhByMeterIdAndDateBetween(meterId, from, to);

        BigDecimal subtotal;
        if (contract.getContractType() == ContractType.FIXED) {
            subtotal = calculateFixed(totalKwh, contract.getFixedPricePerKwhEur());
        } else if (contract.getContractType() == ContractType.FLAT) {
            subtotal = calculateFlat(totalKwh, contract.getFlatMonthlyFeeEur(),
                    contract.getIncludedKwh(), contract.getOveragePricePerKwhEur());
        } else {
            throw new IllegalArgumentException("Unsupported contract type: " + contract.getContractType());
        }

        BigDecimal tax   = subtotal.multiply(contract.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        return new Invoice(
                UUID.randomUUID().toString(),
                period,
                contract.getContractId(),
                meterId,
                contract.getFullName(),
                contract.getContractType(),
                totalKwh.setScale(3, RoundingMode.HALF_UP),
                subtotal,
                tax,
                total,
                LocalDateTime.now()
        );
    }

    /**
     * FIXED calculation per logic-spec.
     * energy = totalKwh * fixedPricePerKwhEur; subtotal = energy
     */
    public BigDecimal calculateFixed(BigDecimal totalKwh, BigDecimal fixedPricePerKwhEur) {
        if (fixedPricePerKwhEur == null) {
            throw new IllegalArgumentException("fixedPricePerKwhEur is required for FIXED contract");
        }
        return totalKwh.multiply(fixedPricePerKwhEur).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * FLAT calculation per logic-spec.
     * base = flatMonthlyFeeEur; overageKwh = max(0, totalKwh - includedKwh);
     * overage = overageKwh * overagePricePerKwhEur; subtotal = base + overage
     */
    public BigDecimal calculateFlat(BigDecimal totalKwh, BigDecimal flatMonthlyFeeEur,
                                    BigDecimal includedKwh, BigDecimal overagePricePerKwhEur) {
        if (flatMonthlyFeeEur == null || includedKwh == null || overagePricePerKwhEur == null) {
            throw new IllegalArgumentException("flatMonthlyFeeEur, includedKwh and overagePricePerKwhEur are required for FLAT contract");
        }
        BigDecimal overageKwh = totalKwh.subtract(includedKwh).max(BigDecimal.ZERO);
        BigDecimal overage    = overageKwh.multiply(overagePricePerKwhEur).setScale(2, RoundingMode.HALF_UP);
        return flatMonthlyFeeEur.add(overage).setScale(2, RoundingMode.HALF_UP);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private YearMonth parseYearMonth(String period) {
        try {
            return YearMonth.parse(period);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid period format '" + period + "'. Expected YYYY-MM");
        }
    }

    /**
     * A contract is active in [from, to] when:
     *   startDate <= to AND (endDate IS NULL OR endDate >= from)
     */
    private List<Contract> findActiveContracts(LocalDate from, LocalDate to) {
        return contractRepo.findAll().stream()
                .filter(c -> !c.getStartDate().isAfter(to)
                        && (c.getEndDate() == null || !c.getEndDate().isBefore(from)))
                .toList();
    }
}
