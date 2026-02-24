package com.naturgy.workshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Naturgy Workshop – Spring Boot entry point.
 *
 * <p>On startup:
 * <ol>
 *   <li>H2 in-memory schema is created (ddl-auto=create-drop)</li>
 *   <li>{@link com.naturgy.workshop.seed.DatabaseSeeder} imports seed CSVs idempotently</li>
 * </ol>
 */
@SpringBootApplication
public class NaturgyWorkshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(NaturgyWorkshopApplication.class, args);
    }
}
