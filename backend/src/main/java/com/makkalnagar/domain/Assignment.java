package com.makkalnagar.domain;

import java.time.LocalDateTime;

/**
 * Audit record of a department assignment.
 */
public class Assignment {
    private final String complaintId;
    private final String departmentName;
    private final String assignedBy;
    private final LocalDateTime assignedOn;
    private final String notes;

    public Assignment(String complaintId, String departmentName, String assignedBy, String notes) {
        this.complaintId = complaintId;
        this.departmentName = departmentName;
        this.assignedBy = assignedBy;
        this.notes = notes;
        this.assignedOn = LocalDateTime.now();
    }

    public String getComplaintId() { return complaintId; }
    public String getDepartmentName() { return departmentName; }
    public String getAssignedBy() { return assignedBy; }
    public LocalDateTime getAssignedOn() { return assignedOn; }
    public String getNotes() { return notes; }

    @Override
    public String toString() {
        return complaintId + " -> " + departmentName + " (" + assignedBy + ") @ " + assignedOn;
    }
}
