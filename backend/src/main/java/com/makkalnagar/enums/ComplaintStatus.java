package com.makkalnagar.enums;

/**
 * Status of a grievance throughout its municipal lifecycle.
 * Valid normal workflow: REGISTERED -> ASSIGNED -> IN_PROGRESS -> RESOLVED.
 */
public enum ComplaintStatus {
    REGISTERED("Registered / In Queue"),
    ASSIGNED("Assigned to Department"),
    IN_PROGRESS("In Progress / Under Field Work"),
    RESOLVED("Resolved");

    private final String displayName;

    ComplaintStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Encapsulated status transition validation.
     */
    public boolean canTransitionTo(ComplaintStatus target) {
        if (target == null) return false;
        switch (this) {
            case REGISTERED:
                return target == ASSIGNED;
            case ASSIGNED:
                return target == IN_PROGRESS;
            case IN_PROGRESS:
                return target == RESOLVED;
            case RESOLVED:
                return false; // Terminal state
            default:
                return false;
        }
    }
}
