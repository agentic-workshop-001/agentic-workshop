package com.naturgy.workshop.domain.repository;

import com.naturgy.workshop.domain.model.Reading;
import com.naturgy.workshop.domain.model.ReadingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data repository for {@link Reading}.
 * PK type: composite {@link ReadingId} (meterId + date + hour).
 */
@Repository
public interface ReadingRepository extends JpaRepository<Reading, ReadingId> {

    /**
     * All readings for a meter within a date range (inclusive).
     * Used by billing service to compute monthly totalKwh.
     */
    List<Reading> findByIdMeterIdAndIdDateBetween(String meterId, LocalDate from, LocalDate to);

    /**
     * Sum of kWh for a meter within a date range.
     * Null-safe: returns 0 when no readings exist.
     */
    @Query("""
           SELECT COALESCE(SUM(r.kwh), 0)
           FROM Reading r
           WHERE r.id.meterId = :meterId
             AND r.id.date BETWEEN :from AND :to
           """)
    BigDecimal sumKwhByMeterIdAndDateBetween(
            @Param("meterId") String meterId,
            @Param("from")    LocalDate from,
            @Param("to")      LocalDate to);
}
