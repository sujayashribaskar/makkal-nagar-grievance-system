package com.makkalnagar.enums;

/**
 * Grievance severity level.
 * Natural ordinal order: LOW (0) < MEDIUM (1) < HIGH (2) < CRITICAL (3).
 * Higher rank represents higher priority.
 */
public enum Severity {
    LOW(1, "Low Priority"),
    MEDIUM(2, "Medium Priority"),
    HIGH(3, "High Priority"),
    CRITICAL(4, "Critical Emergency");

    private final int priorityWeight;
    private final String description;

    Severity(int priorityWeight, String description) {
        this.priorityWeight = priorityWeight;
        this.description = description;
    }

    public int getPriorityWeight() {
        return priorityWeight;
    }

    public String getDescription() {
        return description;
    }
}
