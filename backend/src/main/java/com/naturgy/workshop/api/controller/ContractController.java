package com.naturgy.workshop.api.controller;

import com.naturgy.workshop.domain.model.Contract;
import com.naturgy.workshop.domain.model.Meter;
import com.naturgy.workshop.domain.repository.ContractRepository;
import com.naturgy.workshop.domain.repository.MeterRepository;
import com.naturgy.workshop.service.CsvImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractRepository contractRepo;
    private final MeterRepository    meterRepo;
    private final CsvImportService   csvImport;

    public ContractController(ContractRepository contractRepo,
                              MeterRepository meterRepo,
                              CsvImportService csvImport) {
        this.contractRepo = contractRepo;
        this.meterRepo    = meterRepo;
        this.csvImport    = csvImport;
    }

    @GetMapping
    public List<Contract> findAll(@RequestParam(required = false) String meterId) {
        if (meterId != null) {
            return contractRepo.findByMeter_MeterId(meterId);
        }
        return contractRepo.findAll();
    }

    @GetMapping("/{id}")
    public Contract findById(@PathVariable String id) {
        return contractRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contract not found: " + id));
    }

    @PostMapping
    public ResponseEntity<Contract> create(@RequestBody Contract contract) {
        if (contractRepo.existsById(contract.getContractId())) {
            throw new IllegalArgumentException("Contract already exists: " + contract.getContractId());
        }
        // resolve Meter FK if only meterId is provided in the body
        if (contract.getMeter() != null && contract.getMeter().getMeterId() != null) {
            Meter meter = meterRepo.findById(contract.getMeter().getMeterId())
                    .orElseThrow(() -> new NoSuchElementException("Meter not found: " + contract.getMeter().getMeterId()));
            contract.setMeter(meter);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(contractRepo.save(contract));
    }

    @PutMapping("/{id}")
    public Contract update(@PathVariable String id, @RequestBody Contract contract) {
        if (!contractRepo.existsById(id)) {
            throw new NoSuchElementException("Contract not found: " + id);
        }
        contract.setContractId(id);
        if (contract.getMeter() != null && contract.getMeter().getMeterId() != null) {
            Meter meter = meterRepo.findById(contract.getMeter().getMeterId())
                    .orElseThrow(() -> new NoSuchElementException("Meter not found: " + contract.getMeter().getMeterId()));
            contract.setMeter(meter);
        }
        return contractRepo.save(contract);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!contractRepo.existsById(id)) {
            throw new NoSuchElementException("Contract not found: " + id);
        }
        contractRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<CsvImportService.ImportResult> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        CsvImportService.ImportResult result = csvImport.importContracts(file);
        return ResponseEntity.ok(result);
    }
}
