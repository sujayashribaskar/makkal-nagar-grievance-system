package com.makkalnagar.domain;

import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.Severity;

/**
 * Inheritance & Polymorphism:
 * Extends Grievance and overrides responsibleDepartment() to route to Sanitation & Waste Department.
 */
public class WasteGrievance extends Grievance {

    public WasteGrievance(String complaintId, Citizen citizen, Severity severity,
                          int affectedCount, long arrivalOrder, String location,
                          String description, String openIssueRef) {
        super(complaintId, citizen, Category.WASTE, severity, affectedCount, arrivalOrder,
                location, description, openIssueRef);
    }

    @Override
    public String responsibleDepartment() {
        return "Sanitation and Waste Management Department";
    }
}
