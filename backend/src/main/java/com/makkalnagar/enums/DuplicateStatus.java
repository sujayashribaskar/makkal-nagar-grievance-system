package com.makkalnagar.enums;

/**
 * Status of a duplicate complaint relationship.
 * Allows the system to distinguish a possible duplicate from a confirmed duplicate relationship.
 */
public enum DuplicateStatus {
    FLAGGED("Possible Duplicate"),
    POSSIBLE_DUPLICATE("Possible Duplicate"),
    CONFIRMED("Confirmed Duplicate"),
    CONFIRMED_DUPLICATE("Confirmed Duplicate"),
    DISMISSED("Dismissed (Not Duplicate)");

    private final String description;

    DuplicateStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
