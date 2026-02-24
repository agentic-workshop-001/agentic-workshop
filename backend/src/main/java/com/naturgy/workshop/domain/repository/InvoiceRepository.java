package com.naturgy.workshop.domain.repository;

import com.naturgy.workshop.domain.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    List<Invoice> findByPeriod(String period);

    Optional<Invoice> findByContractIdAndPeriod(String contractId, String period);

    List<Invoice> findAllByOrderByPeriodDescGeneratedAtDesc();
}
