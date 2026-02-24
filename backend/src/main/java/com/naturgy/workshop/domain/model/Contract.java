package com.naturgy.workshop.domain.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.naturgy.workshop.domain.enums.BillingCycle;
import com.naturgy.workshop.domain.enums.ContractType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Energy supply contract between a customer and a meter.
 *
 * <p>Field-level constraints (csv-spec + clarifications):
 * <ul>
 *   <li>FIXED contracts: {@code fixedPricePerKwhEur} populated; flat-rate fields NULL</li>
 *   <li>FLAT  contracts: {@code flatMonthlyFeeEur}, {@code includedKwh}, {@code overagePricePerKwhEur} populated;
 *       {@code fixedPricePerKwhEur} NULL</li>
 * </ul>
 * The DB layer stores all nullable columns as-is; semantic validation belongs to the billing service.
 */
@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @Column(name = "contract_id", nullable = false, length = 50)
    @NotBlank
    private String contractId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    @NotNull
    @JsonBackReference("meter-contracts")
    private Meter meter;

    @Column(name = "customer_id", nullable = false, length = 50)
    @NotBlank
    private String customerId;

    @Column(name = "full_name", nullable = false)
    @NotBlank
    private String fullName;

    @Column(name = "nif", nullable = false, length = 20)
    @NotBlank
    private String nif;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 10)
    @NotNull
    private ContractType contractType;

    @Column(name = "start_date", nullable = false)
    @NotNull
    private LocalDate startDate;

    /** NULL means open-ended contract (no end date in CSV). */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 10)
    @NotNull
    private BillingCycle billingCycle;

    // ── FLAT-contract fields (NULL for FIXED) ─────────────────────────────────

    @Column(name = "flat_monthly_fee_eur", precision = 10, scale = 2)
    private BigDecimal flatMonthlyFeeEur;

    @Column(name = "included_kwh", precision = 10, scale = 3)
    private BigDecimal includedKwh;

    @Column(name = "overage_price_per_kwh_eur", precision = 10, scale = 4)
    private BigDecimal overagePricePerKwhEur;

    // ── FIXED-contract field (NULL for FLAT) ──────────────────────────────────

    @Column(name = "fixed_price_per_kwh_eur", precision = 10, scale = 4)
    private BigDecimal fixedPricePerKwhEur;

    // ── Common ────────────────────────────────────────────────────────────────

    /** Tax rate stored as a decimal fraction, e.g. 0.21 for 21%. */
    @Column(name = "tax_rate", nullable = false, precision = 6, scale = 4)
    @NotNull
    private BigDecimal taxRate;

    @Column(name = "iban", length = 34)
    private String iban;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected Contract() {}

    public Contract(String contractId, Meter meter, String customerId, String fullName,
                    String nif, String email, ContractType contractType,
                    LocalDate startDate, LocalDate endDate, BillingCycle billingCycle,
                    BigDecimal flatMonthlyFeeEur, BigDecimal includedKwh,
                    BigDecimal overagePricePerKwhEur, BigDecimal fixedPricePerKwhEur,
                    BigDecimal taxRate, String iban) {
        this.contractId            = contractId;
        this.meter                 = meter;
        this.customerId            = customerId;
        this.fullName              = fullName;
        this.nif                   = nif;
        this.email                 = email;
        this.contractType          = contractType;
        this.startDate             = startDate;
        this.endDate               = endDate;
        this.billingCycle          = billingCycle;
        this.flatMonthlyFeeEur     = flatMonthlyFeeEur;
        this.includedKwh           = includedKwh;
        this.overagePricePerKwhEur = overagePricePerKwhEur;
        this.fixedPricePerKwhEur   = fixedPricePerKwhEur;
        this.taxRate               = taxRate;
        this.iban                  = iban;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String       getContractId()            { return contractId; }
    public void         setContractId(String v)    { this.contractId = v; }

    public Meter        getMeter()                 { return meter; }
    public void         setMeter(Meter v)          { this.meter = v; }

    public String       getCustomerId()            { return customerId; }
    public void         setCustomerId(String v)    { this.customerId = v; }

    public String       getFullName()              { return fullName; }
    public void         setFullName(String v)      { this.fullName = v; }

    public String       getNif()                   { return nif; }
    public void         setNif(String v)           { this.nif = v; }

    public String       getEmail()                 { return email; }
    public void         setEmail(String v)         { this.email = v; }

    public ContractType getContractType()          { return contractType; }
    public void         setContractType(ContractType v) { this.contractType = v; }

    public LocalDate    getStartDate()             { return startDate; }
    public void         setStartDate(LocalDate v)  { this.startDate = v; }

    public LocalDate    getEndDate()               { return endDate; }
    public void         setEndDate(LocalDate v)    { this.endDate = v; }

    public BillingCycle getBillingCycle()          { return billingCycle; }
    public void         setBillingCycle(BillingCycle v) { this.billingCycle = v; }

    public BigDecimal   getFlatMonthlyFeeEur()     { return flatMonthlyFeeEur; }
    public void         setFlatMonthlyFeeEur(BigDecimal v) { this.flatMonthlyFeeEur = v; }

    public BigDecimal   getIncludedKwh()           { return includedKwh; }
    public void         setIncludedKwh(BigDecimal v) { this.includedKwh = v; }

    public BigDecimal   getOveragePricePerKwhEur() { return overagePricePerKwhEur; }
    public void         setOveragePricePerKwhEur(BigDecimal v) { this.overagePricePerKwhEur = v; }

    public BigDecimal   getFixedPricePerKwhEur()   { return fixedPricePerKwhEur; }
    public void         setFixedPricePerKwhEur(BigDecimal v) { this.fixedPricePerKwhEur = v; }

    public BigDecimal   getTaxRate()               { return taxRate; }
    public void         setTaxRate(BigDecimal v)   { this.taxRate = v; }

    public String       getIban()                  { return iban; }
    public void         setIban(String v)          { this.iban = v; }

    @Override
    public String toString() {
        return "Contract{contractId='" + contractId + "', meterId='" + meter.getMeterId()
               + "', type=" + contractType + '}';
    }
}
