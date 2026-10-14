package com.makkalnagar.domain;

import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.Severity;

/**
 * Inheritance & Polymorphism:
 * Extends Grievance and overrides responsibleDepartment() to route to Streetlight Department.
 */
public class StreetlightGrievance extends Grievance {

    public StreetlightGrievance(String complaintId, Citizen citizen, Severity severity,
                                int affectedCount, long arrivalOrder, String location,
                                String description, String openIssueRef) {
        super(complaintId, citizen, Category.STREETLIGHT, severity, affectedCount, arrivalOrder,
                location, description, openIssueRef);
    }

    @Override
    public String responsibleDepartment() {
        return "Streetlight and Electrical Department";
    }
}
