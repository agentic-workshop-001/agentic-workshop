package com.naturgy.workshop.controller;

import com.naturgy.workshop.entity.Meter;
import com.naturgy.workshop.exception.ResourceNotFoundException;
import com.naturgy.workshop.exception.ValidationException;
import com.naturgy.workshop.repository.MeterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meters")
public class MeterController {

    private final MeterRepository meterRepo;

    public MeterController(MeterRepository meterRepo) {
        this.meterRepo = meterRepo;
    }

    @GetMapping
    public List<Meter> getAll() {
        return meterRepo.findAll();
    }

    @GetMapping("/{meterId}")
    public Meter getById(@PathVariable String meterId) {
        return meterRepo.findById(meterId)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found: " + meterId));
    }

    @PostMapping
    public ResponseEntity<Meter> create(@RequestBody Meter meter) {
        if (meter.getMeterId() == null || meter.getMeterId().isBlank()) {
            throw new ValidationException("meterId is required");
        }
        if (meterRepo.existsById(meter.getMeterId())) {
            throw new ValidationException("Meter already exists: " + meter.getMeterId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(meterRepo.save(meter));
    }

    @PutMapping("/{meterId}")
    public Meter update(@PathVariable String meterId, @RequestBody Meter meter) {
        meterRepo.findById(meterId)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found: " + meterId));
        meter.setMeterId(meterId);
        return meterRepo.save(meter);
    }

    @DeleteMapping("/{meterId}")
    public ResponseEntity<Void> delete(@PathVariable String meterId) {
        meterRepo.findById(meterId)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found: " + meterId));
        meterRepo.deleteById(meterId);
        return ResponseEntity.noContent().build();
    }
}
