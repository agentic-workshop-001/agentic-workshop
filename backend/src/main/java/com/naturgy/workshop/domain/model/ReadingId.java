package com.naturgy.workshop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite primary key for {@link Reading}.
 *
 * <p>Uniqueness rule (csv-spec): same meterId + date + hour is a duplicate.
 * The DB enforces this via the PK constraint on these three columns.
 */
@Embeddable
public class ReadingId implements Serializable {

    @Column(name = "meter_id", nullable = false, length = 50)
    private String meterId;

    @Column(name = "reading_date", nullable = false)
    private LocalDate date;

    /**
     * Hour of day: 0–23.
     * Column named "reading_hour" because H2 2.2.x reserves the identifier HOUR
     * as a datetime keyword (used in EXTRACT expressions).
     */
    @Column(name = "reading_hour", nullable = false)
    private Integer hour;

    protected ReadingId() {}

    public ReadingId(String meterId, LocalDate date, Integer hour) {
        this.meterId = meterId;
        this.date    = date;
        this.hour    = hour;
    }

    public String    getMeterId() { return meterId; }
    public LocalDate getDate()    { return date; }
    public Integer   getHour()    { return hour; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReadingId that)) return false;
        return Objects.equals(meterId, that.meterId)
            && Objects.equals(date,    that.date)
            && Objects.equals(hour,    that.hour);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meterId, date, hour);
    }

    @Override
    public String toString() {
        return meterId + "@" + date + "h" + hour;
    }
}
