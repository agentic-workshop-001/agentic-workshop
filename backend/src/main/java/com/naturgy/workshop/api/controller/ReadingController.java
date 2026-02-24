package com.naturgy.workshop.api.controller;

import com.naturgy.workshop.domain.model.Reading;
import com.naturgy.workshop.domain.model.ReadingId;
import com.naturgy.workshop.domain.repository.ReadingRepository;
import com.naturgy.workshop.service.CsvImportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/readings")
public class ReadingController {

    private final ReadingRepository readingRepo;
    private final CsvImportService  csvImport;

    public ReadingController(ReadingRepository readingRepo, CsvImportService csvImport) {
        this.readingRepo = readingRepo;
        this.csvImport   = csvImport;
    }

    @GetMapping
    public List<Reading> findAll(
            @RequestParam(required = false) String meterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (meterId != null && from != null && to != null) {
            return readingRepo.findByIdMeterIdAndIdDateBetween(meterId, from, to);
        }
        if (meterId != null) {
            return readingRepo.findByIdMeterIdAndIdDateBetween(meterId,
                    LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31));
        }
        return readingRepo.findAll();
    }

    @GetMapping("/{meterId}/{date}/{hour}")
    public Reading findById(@PathVariable String meterId,
                            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @PathVariable Integer hour) {
        ReadingId rid = new ReadingId(meterId, date, hour);
        return readingRepo.findById(rid)
                .orElseThrow(() -> new NoSuchElementException(
                        "Reading not found: " + meterId + "/" + date + "/" + hour));
    }

    @PostMapping
    public ResponseEntity<Reading> create(@RequestBody Reading reading) {
        if (readingRepo.existsById(reading.getId())) {
            throw new IllegalArgumentException("Reading already exists: " + reading.getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(readingRepo.save(reading));
    }

    @DeleteMapping("/{meterId}/{date}/{hour}")
    public ResponseEntity<Void> delete(@PathVariable String meterId,
                                       @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                       @PathVariable Integer hour) {
        ReadingId rid = new ReadingId(meterId, date, hour);
        if (!readingRepo.existsById(rid)) {
            throw new NoSuchElementException("Reading not found: " + meterId + "/" + date + "/" + hour);
        }
        readingRepo.deleteById(rid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<CsvImportService.ImportResult> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        CsvImportService.ImportResult result = csvImport.importReadings(file);
        return ResponseEntity.ok(result);
    }
}
