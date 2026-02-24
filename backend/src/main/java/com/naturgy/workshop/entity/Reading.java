package com.naturgy.workshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "readings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"meter_id", "date", "reading_hour"}))
@IdClass(ReadingId.class)
public class Reading {

    @Id
    @Column(name = "meter_id", nullable = false)
    private String meterId;

    @Id
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Id
    @Column(name = "reading_hour", nullable = false)
    private Integer hour;

    @Column(name = "kwh", nullable = false, precision = 10, scale = 3)
    private BigDecimal kwh;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality")
    private Quality quality;

    public Reading() {}

    public String getMeterId() { return meterId; }
    public void setMeterId(String meterId) { this.meterId = meterId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Integer getHour() { return hour; }
    public void setHour(Integer hour) { this.hour = hour; }
    public BigDecimal getKwh() { return kwh; }
    public void setKwh(BigDecimal kwh) { this.kwh = kwh; }
    public Quality getQuality() { return quality; }
    public void setQuality(Quality quality) { this.quality = quality; }
}
