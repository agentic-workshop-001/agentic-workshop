package com.naturgy.workshop.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * Physical energy meter (punto de suministro).
 * PK: meterId  –  e.g. "MTR0001"
 * cups is optional per csv-spec.
 */
@Entity
@Table(name = "meters")
public class Meter {

    /** Business key – assigned externally, never auto-generated. */
    @Id
    @Column(name = "meter_id", nullable = false, length = 50)
    @NotBlank
    private String meterId;

    /** CUPS – supply point identifier (optional). */
    @Column(name = "cups", length = 30)
    private String cups;

    @Column(name = "address", nullable = false)
    @NotBlank
    private String address;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "city", nullable = false)
    @NotBlank
    private String city;

    /** Contracts that reference this meter (1 active per period per logic-spec). */
    @OneToMany(mappedBy = "meter", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Contract> contracts = new ArrayList<>();

    /** Hourly readings registered on this meter. */
    @OneToMany(mappedBy = "meter", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Reading> readings = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    protected Meter() {}

    public Meter(String meterId, String cups, String address, String postalCode, String city) {
        this.meterId    = meterId;
        this.cups       = cups;
        this.address    = address;
        this.postalCode = postalCode;
        this.city       = city;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getMeterId()    { return meterId; }
    public void   setMeterId(String meterId) { this.meterId = meterId; }

    public String getCups()       { return cups; }
    public void   setCups(String cups) { this.cups = cups; }

    public String getAddress()    { return address; }
    public void   setAddress(String address) { this.address = address; }

    public String getPostalCode() { return postalCode; }
    public void   setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCity()       { return city; }
    public void   setCity(String city) { this.city = city; }

    public List<Contract> getContracts() { return contracts; }
    public List<Reading>  getReadings()  { return readings; }

    @Override
    public String toString() {
        return "Meter{meterId='" + meterId + "', cups='" + cups + "', city='" + city + "'}";
    }
}
