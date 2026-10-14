package com.makkalnagar.enums;

/**
 * Complaint categories handled by Makkal Nagar Municipal Corporation.
 */
public enum Category {
    WATER("Water Supply", "Water Supply Department"),
    ROAD("Roads", "Roads and Infrastructure Department"),
    STREETLIGHT("Streetlights", "Streetlight and Electrical Department"),
    WASTE("Waste Management", "Sanitation and Waste Management Department");

    private final String displayName;
    private final String defaultDepartmentName;

    Category(String displayName, String defaultDepartmentName) {
        this.displayName = displayName;
        this.defaultDepartmentName = defaultDepartmentName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultDepartmentName() {
        return defaultDepartmentName;
    }
}
