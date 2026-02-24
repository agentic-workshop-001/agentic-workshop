package com.naturgy.workshop.controller;

import com.naturgy.workshop.entity.Contract;
import com.naturgy.workshop.exception.ResourceNotFoundException;
import com.naturgy.workshop.exception.ValidationException;
import com.naturgy.workshop.repository.ContractRepository;
import com.naturgy.workshop.repository.MeterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractRepository contractRepo;
    private final MeterRepository meterRepo;

    public ContractController(ContractRepository contractRepo, MeterRepository meterRepo) {
        this.contractRepo = contractRepo;
        this.meterRepo = meterRepo;
    }

    @GetMapping
    public List<Contract> getAll() {
        return contractRepo.findAll();
    }

    @GetMapping("/{contractId}")
    public Contract getById(@PathVariable String contractId) {
        return contractRepo.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
    }

    @PostMapping
    public ResponseEntity<Contract> create(@RequestBody Contract contract) {
        if (contract.getContractId() == null || contract.getContractId().isBlank()) {
            throw new ValidationException("contractId is required");
        }
        if (contractRepo.existsById(contract.getContractId())) {
            throw new ValidationException("Contract already exists: " + contract.getContractId());
        }
        if (!meterRepo.existsById(contract.getMeterId())) {
            throw new ValidationException("Meter not found: " + contract.getMeterId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(contractRepo.save(contract));
    }

    @PutMapping("/{contractId}")
    public Contract update(@PathVariable String contractId, @RequestBody Contract contract) {
        contractRepo.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
        contract.setContractId(contractId);
        return contractRepo.save(contract);
    }

    @DeleteMapping("/{contractId}")
    public ResponseEntity<Void> delete(@PathVariable String contractId) {
        contractRepo.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
        contractRepo.deleteById(contractId);
        return ResponseEntity.noContent().build();
    }
}
