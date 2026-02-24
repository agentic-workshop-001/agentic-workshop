package com.naturgy.workshop.domain.enums;

/**
 * Billing contract type.
 * <p>
 * FIXED – customer pays a per-kWh rate on all consumed energy.<br>
 * FLAT  – customer pays a flat monthly fee that covers a block of kWh;
 *          excess consumption is billed at the overage rate.
 */
public enum ContractType {
    FIXED,
    FLAT
}
