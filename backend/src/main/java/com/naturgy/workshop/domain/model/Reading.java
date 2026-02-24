package com.naturgy.workshop.domain.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.naturgy.workshop.domain.enums.ReadingQuality;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Hourly energy reading registered by a meter.
 *
 * <p>PK is composite: (meterId, date, hour) — enforced by {@link ReadingId}.
 * Duplicate detection (same PK) is handled at seed time by an existence check
 * before insert (clarifications.txt).
 */
@Entity
@Table(name = "readings")
public class Reading {

    @EmbeddedId
    private ReadingId id;

    /**
     * FK back to {@link Meter}.
     * {@code insertable=false, updatable=false} because meter_id is already managed
     * by the embedded PK column.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", insertable = false, updatable = false)
    @NotNull
    @JsonBackReference("meter-readings")
    private Meter meter;

    /** Energy consumed in this hour, in kWh.  Must be >= 0 per csv-spec. */
    @Column(name = "kwh", nullable = false, precision = 10, scale = 3)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal kwh;

    /** Optional quality flag.  NULL is allowed (clarifications.txt). */
    @Enumerated(EnumType.STRING)
    @Column(name = "quality", length = 10)
    private ReadingQuality quality;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected Reading() {}

    public Reading(ReadingId id, Meter meter, BigDecimal kwh, ReadingQuality quality) {
        this.id      = id;
        this.meter   = meter;
        this.kwh     = kwh;
        this.quality = quality;
    }

    // ── Convenience accessors ─────────────────────────────────────────────────

    public ReadingId      getId()      { return id; }
    public void           setId(ReadingId id) { this.id = id; }

    public Meter          getMeter()   { return meter; }
    public void           setMeter(Meter meter) { this.meter = meter; }

    public BigDecimal     getKwh()     { return kwh; }
    public void           setKwh(BigDecimal kwh) { this.kwh = kwh; }

    public ReadingQuality getQuality() { return quality; }
    public void           setQuality(ReadingQuality quality) { this.quality = quality; }

    @Override
    public String toString() {
        return "Reading{id=" + id + ", kwh=" + kwh + ", quality=" + quality + '}';
    }
}
