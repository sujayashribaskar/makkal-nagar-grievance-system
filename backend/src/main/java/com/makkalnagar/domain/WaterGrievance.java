package com.makkalnagar.domain;

import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.Severity;

/**
 * Inheritance & Polymorphism:
 * Extends Grievance and overrides responsibleDepartment() to route to Water Supply Department.
 */
public class WaterGrievance extends Grievance {

    public WaterGrievance(String complaintId, Citizen citizen, Severity severity,
                          int affectedCount, long arrivalOrder, String location,
                          String description, String openIssueRef) {
        super(complaintId, citizen, Category.WATER, severity, affectedCount, arrivalOrder,
                location, description, openIssueRef);
    }

    @Override
    public String responsibleDepartment() {
        return "Water Supply Department";
    }
}
