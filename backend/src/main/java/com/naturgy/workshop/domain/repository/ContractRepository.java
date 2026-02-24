package com.naturgy.workshop.domain.repository;

import com.naturgy.workshop.domain.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data repository for {@link Contract}.
 * PK type: String (contractId).
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, String> {

    /**
     * Find all contracts for a given meter.
     * Useful for billing-service: it will filter active contracts for a period.
     */
    List<Contract> findByMeter_MeterId(String meterId);

    /**
     * Find contracts for a meter that were active on a specific date.
     * A contract is active when startDate <= date AND (endDate IS NULL OR endDate >= date).
     * Named query equivalent done with explicit JPQL in service layer — this helper
     * covers the common case for the billing sprint.
     */
    List<Contract> findByMeter_MeterIdAndStartDateLessThanEqualAndEndDateIsNull(
            String meterId, LocalDate date);

    List<Contract> findByMeter_MeterIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String meterId, LocalDate startDate, LocalDate endDate);
}
