package com.naturgy.workshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @Column(name = "contract_id")
    private String contractId;

    @Column(name = "meter_id", nullable = false)
    private String meterId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "nif", nullable = false)
    private String nif;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false)
    private ContractType contractType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @Column(name = "flat_monthly_fee_eur", precision = 10, scale = 2)
    private BigDecimal flatMonthlyFeeEur;

    @Column(name = "included_kwh", precision = 10, scale = 3)
    private BigDecimal includedKwh;

    @Column(name = "overage_price_per_kwh_eur", precision = 10, scale = 4)
    private BigDecimal overagePricePerKwhEur;

    @Column(name = "fixed_price_per_kwh_eur", precision = 10, scale = 4)
    private BigDecimal fixedPricePerKwhEur;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "iban")
    private String iban;

    public Contract() {}

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
    public String getMeterId() { return meterId; }
    public void setMeterId(String meterId) { this.meterId = meterId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public ContractType getContractType() { return contractType; }
    public void setContractType(ContractType contractType) { this.contractType = contractType; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BillingCycle getBillingCycle() { return billingCycle; }
    public void setBillingCycle(BillingCycle billingCycle) { this.billingCycle = billingCycle; }
    public BigDecimal getFlatMonthlyFeeEur() { return flatMonthlyFeeEur; }
    public void setFlatMonthlyFeeEur(BigDecimal flatMonthlyFeeEur) { this.flatMonthlyFeeEur = flatMonthlyFeeEur; }
    public BigDecimal getIncludedKwh() { return includedKwh; }
    public void setIncludedKwh(BigDecimal includedKwh) { this.includedKwh = includedKwh; }
    public BigDecimal getOveragePricePerKwhEur() { return overagePricePerKwhEur; }
    public void setOveragePricePerKwhEur(BigDecimal overagePricePerKwhEur) { this.overagePricePerKwhEur = overagePricePerKwhEur; }
    public BigDecimal getFixedPricePerKwhEur() { return fixedPricePerKwhEur; }
    public void setFixedPricePerKwhEur(BigDecimal fixedPricePerKwhEur) { this.fixedPricePerKwhEur = fixedPricePerKwhEur; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
}
