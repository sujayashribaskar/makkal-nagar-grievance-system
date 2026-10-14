package com.makkalnagar.enums;

/**
 * Status of a flagged duplicate complaint candidate.
 */
public enum DuplicateStatus {
    FLAGGED("Flagged for Review"),
    CONFIRMED("Confirmed Duplicate"),
    DISMISSED("Dismissed (Not Duplicate)");

    private final String description;

    DuplicateStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
