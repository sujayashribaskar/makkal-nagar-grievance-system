package com.makkalnagar.domain;

import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.Severity;

/**
 * Inheritance & Polymorphism:
 * Extends Grievance and overrides responsibleDepartment() to route to Roads Department.
 */
public class RoadGrievance extends Grievance {

    public RoadGrievance(String complaintId, Citizen citizen, Severity severity,
                         int affectedCount, long arrivalOrder, String location,
                         String description, String openIssueRef) {
        super(complaintId, citizen, Category.ROAD, severity, affectedCount, arrivalOrder,
                location, description, openIssueRef);
    }

    @Override
    public String responsibleDepartment() {
        return "Roads and Infrastructure Department";
    }
}
