package com.naturgy.workshop.domain.enums;

/**
 * Measurement quality indicator on a reading.
 * NULL is allowed (clarifications.txt – Quality Field).
 */
public enum ReadingQuality {
    REAL,
    ESTIMATED
}
