package com.naturgy.workshop.repository;

import com.naturgy.workshop.entity.Reading;
import com.naturgy.workshop.entity.ReadingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReadingRepository extends JpaRepository<Reading, ReadingId> {

    List<Reading> findByMeterId(String meterId);

    @Query("SELECT COALESCE(SUM(r.kwh), 0) FROM Reading r WHERE r.meterId = :meterId AND r.date >= :start AND r.date <= :end")
    BigDecimal sumKwhByMeterIdAndDateBetween(
            @Param("meterId") String meterId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
