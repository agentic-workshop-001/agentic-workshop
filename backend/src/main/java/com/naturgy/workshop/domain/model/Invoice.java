package com.naturgy.workshop.domain.model;

import com.naturgy.workshop.domain.enums.ContractType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Generated invoice for a contract in a billing period.
 */
@Entity
@Table(name = "invoices",
       uniqueConstraints = @UniqueConstraint(columnNames = {"contract_id", "period"}))
public class Invoice {

    @Id
    @Column(name = "invoice_id", nullable = false, length = 50)
    @NotBlank
    private String invoiceId;

    /** YYYY-MM billing period. */
    @Column(name = "period", nullable = false, length = 7)
    @NotBlank
    private String period;

    @Column(name = "contract_id", nullable = false, length = 50)
    @NotBlank
    private String contractId;

    @Column(name = "meter_id", nullable = false, length = 50)
    @NotBlank
    private String meterId;

    @Column(name = "customer_full_name", nullable = false)
    @NotBlank
    private String customerFullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 10)
    @NotNull
    private ContractType contractType;

    @Column(name = "total_kwh", nullable = false, precision = 12, scale = 3)
    @NotNull
    private BigDecimal totalKwh;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    @NotNull
    private BigDecimal subtotal;

    @Column(name = "tax", nullable = false, precision = 10, scale = 2)
    @NotNull
    private BigDecimal tax;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    @NotNull
    private BigDecimal total;

    @Column(name = "generated_at", nullable = false)
    @NotNull
    private LocalDateTime generatedAt;

    protected Invoice() {}

    public Invoice(String invoiceId, String period, String contractId, String meterId,
                   String customerFullName, ContractType contractType,
                   BigDecimal totalKwh, BigDecimal subtotal, BigDecimal tax, BigDecimal total,
                   LocalDateTime generatedAt) {
        this.invoiceId        = invoiceId;
        this.period           = period;
        this.contractId       = contractId;
        this.meterId          = meterId;
        this.customerFullName = customerFullName;
        this.contractType     = contractType;
        this.totalKwh         = totalKwh;
        this.subtotal         = subtotal;
        this.tax              = tax;
        this.total            = total;
        this.generatedAt      = generatedAt;
    }

    public String        getInvoiceId()        { return invoiceId; }
    public String        getPeriod()           { return period; }
    public String        getContractId()       { return contractId; }
    public String        getMeterId()          { return meterId; }
    public String        getCustomerFullName() { return customerFullName; }
    public ContractType  getContractType()     { return contractType; }
    public BigDecimal    getTotalKwh()         { return totalKwh; }
    public BigDecimal    getSubtotal()         { return subtotal; }
    public BigDecimal    getTax()              { return tax; }
    public BigDecimal    getTotal()            { return total; }
    public LocalDateTime getGeneratedAt()      { return generatedAt; }
}
