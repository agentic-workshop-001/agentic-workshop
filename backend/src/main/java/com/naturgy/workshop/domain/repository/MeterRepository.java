package com.naturgy.workshop.domain.repository;

import com.naturgy.workshop.domain.model.Meter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link Meter}.
 * PK type: String (meterId).
 */
@Repository
public interface MeterRepository extends JpaRepository<Meter, String> {
}
