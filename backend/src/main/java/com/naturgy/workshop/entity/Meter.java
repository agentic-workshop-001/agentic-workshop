package com.naturgy.workshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "meters")
public class Meter {

    @Id
    @Column(name = "meter_id")
    private String meterId;

    @Column(name = "cups")
    private String cups;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "city", nullable = false)
    private String city;

    public Meter() {}

    public String getMeterId() { return meterId; }
    public void setMeterId(String meterId) { this.meterId = meterId; }
    public String getCups() { return cups; }
    public void setCups(String cups) { this.cups = cups; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
