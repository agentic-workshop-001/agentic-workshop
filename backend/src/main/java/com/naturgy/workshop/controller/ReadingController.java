package com.naturgy.workshop.controller;

import com.naturgy.workshop.entity.Reading;
import com.naturgy.workshop.entity.ReadingId;
import com.naturgy.workshop.exception.ResourceNotFoundException;
import com.naturgy.workshop.exception.ValidationException;
import com.naturgy.workshop.repository.MeterRepository;
import com.naturgy.workshop.repository.ReadingRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/readings")
public class ReadingController {

    private final ReadingRepository readingRepo;
    private final MeterRepository meterRepo;

    public ReadingController(ReadingRepository readingRepo, MeterRepository meterRepo) {
        this.readingRepo = readingRepo;
        this.meterRepo = meterRepo;
    }

    @GetMapping
    public List<Reading> getAll(
            @RequestParam(required = false) String meterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer hour) {
        if (meterId != null && date != null && hour != null) {
            return readingRepo.findById(new ReadingId(meterId, date, hour))
                    .map(List::of)
                    .orElse(List.of());
        }
        if (meterId != null) {
            return readingRepo.findByMeterId(meterId);
        }
        return readingRepo.findAll();
    }

    @PostMapping
    public ResponseEntity<Reading> create(@RequestBody Reading reading) {
        validateReading(reading);
        ReadingId rid = new ReadingId(reading.getMeterId(), reading.getDate(), reading.getHour());
        if (readingRepo.existsById(rid)) {
            throw new ValidationException("Duplicate reading: " + reading.getMeterId()
                    + " " + reading.getDate() + " hour=" + reading.getHour());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(readingRepo.save(reading));
    }

    @PutMapping
    public Reading update(
            @RequestParam String meterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer hour,
            @RequestBody Reading reading) {
        ReadingId rid = new ReadingId(meterId, date, hour);
        readingRepo.findById(rid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reading not found: " + meterId + " " + date + " hour=" + hour));
        reading.setMeterId(meterId);
        reading.setDate(date);
        reading.setHour(hour);
        return readingRepo.save(reading);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @RequestParam String meterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer hour) {
        ReadingId rid = new ReadingId(meterId, date, hour);
        readingRepo.findById(rid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reading not found: " + meterId + " " + date + " hour=" + hour));
        readingRepo.deleteById(rid);
        return ResponseEntity.noContent().build();
    }

    private void validateReading(Reading r) {
        if (r.getMeterId() == null || r.getMeterId().isBlank()) {
            throw new ValidationException("meterId is required");
        }
        if (!meterRepo.existsById(r.getMeterId())) {
            throw new ValidationException("Meter not found: " + r.getMeterId());
        }
        if (r.getDate() == null) {
            throw new ValidationException("date is required");
        }
        if (r.getHour() == null || r.getHour() < 0 || r.getHour() > 23) {
            throw new ValidationException("hour must be in [0..23]");
        }
        if (r.getKwh() == null || r.getKwh().signum() < 0) {
            throw new ValidationException("kwh must be >= 0");
        }
    }
}
