package com.naturgy.workshop.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class ReadingId implements Serializable {

    private String meterId;
    private LocalDate date;
    private Integer hour;

    public ReadingId() {}

    public ReadingId(String meterId, LocalDate date, Integer hour) {
        this.meterId = meterId;
        this.date = date;
        this.hour = hour;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReadingId other)) return false;
        return Objects.equals(meterId, other.meterId)
                && Objects.equals(date, other.date)
                && Objects.equals(hour, other.hour);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meterId, date, hour);
    }

    public String getMeterId() { return meterId; }
    public void setMeterId(String meterId) { this.meterId = meterId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Integer getHour() { return hour; }
    public void setHour(Integer hour) { this.hour = hour; }
}
