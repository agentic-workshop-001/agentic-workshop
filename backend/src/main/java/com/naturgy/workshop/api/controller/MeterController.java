package com.naturgy.workshop.api.controller;

import com.naturgy.workshop.domain.model.Meter;
import com.naturgy.workshop.domain.repository.MeterRepository;
import com.naturgy.workshop.service.CsvImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/meters")
public class MeterController {

    private final MeterRepository    meterRepo;
    private final CsvImportService   csvImport;

    public MeterController(MeterRepository meterRepo, CsvImportService csvImport) {
        this.meterRepo = meterRepo;
        this.csvImport = csvImport;
    }

    @GetMapping
    public List<Meter> findAll() {
        return meterRepo.findAll();
    }

    @GetMapping("/{id}")
    public Meter findById(@PathVariable String id) {
        return meterRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Meter not found: " + id));
    }

    @PostMapping
    public ResponseEntity<Meter> create(@RequestBody Meter meter) {
        if (meterRepo.existsById(meter.getMeterId())) {
            throw new IllegalArgumentException("Meter already exists: " + meter.getMeterId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(meterRepo.save(meter));
    }

    @PutMapping("/{id}")
    public Meter update(@PathVariable String id, @RequestBody Meter meter) {
        if (!meterRepo.existsById(id)) {
            throw new NoSuchElementException("Meter not found: " + id);
        }
        meter.setMeterId(id);
        return meterRepo.save(meter);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!meterRepo.existsById(id)) {
            throw new NoSuchElementException("Meter not found: " + id);
        }
        meterRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<CsvImportService.ImportResult> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        CsvImportService.ImportResult result = csvImport.importMeters(file);
        return ResponseEntity.ok(result);
    }
}
