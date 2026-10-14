package com.makkalnagar.domain;

import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.enums.Severity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract Base Class demonstrating core OOP concepts:
 * 1. Abstraction: Defines the contract for all municipal grievances;
 *    abstract method responsibleDepartment() defines department resolution.
 * 2. Encapsulation: Internal state is strictly protected via private fields;
 *    updateStatus() is the only legal way to transition state and records audit history.
 * 3. Composition: Owns an audit list of StatusUpdate objects.
 */
public abstract class Grievance {

    private final String complaintId;
    private final Citizen citizen;
    private final Category category;
    private final Severity severity;
    private final int affectedCount;
    private final long arrivalOrder;
    private final String location;
    private final String description;
    private final String openIssueRef;

    private ComplaintStatus status;
    private String assignedDepartment;
    private final List<StatusUpdate> history;

    protected Grievance(String complaintId, Citizen citizen, Category category,
                         Severity severity, int affectedCount, long arrivalOrder,
                         String location, String description, String openIssueRef) {
        if (complaintId == null || complaintId.isBlank()) {
            throw new IllegalArgumentException("Complaint ID cannot be null or blank");
        }
        if (citizen == null) {
            throw new IllegalArgumentException("Citizen reference cannot be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity cannot be null");
        }
        if (affectedCount < 1) {
            throw new IllegalArgumentException("Affected count must be at least 1");
        }

        this.complaintId = complaintId;
        this.citizen = citizen;
        this.category = category;
        this.severity = severity;
        this.affectedCount = affectedCount;
        this.arrivalOrder = arrivalOrder;
        this.location = location;
        this.description = description;
        this.openIssueRef = openIssueRef;
        this.status = ComplaintStatus.REGISTERED;
        this.history = new ArrayList<>();
        this.history.add(new StatusUpdate(ComplaintStatus.REGISTERED, "Grievance registered in municipal portal", "CITIZEN"));
    }

    /**
     * Polymorphic method: overridden by each grievance subclass to declare its responsible department.
     */
    public abstract String responsibleDepartment();

    /**
     * Encapsulated status transition. Validates permitted lifecycle:
     * REGISTERED -> ASSIGNED -> IN_PROGRESS -> RESOLVED.
     */
    public void updateStatus(ComplaintStatus newStatus, String remarks, String actor) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Illegal status transition for " + complaintId + ": " + this.status + " -> " + newStatus);
        }
        this.status = newStatus;
        this.history.add(new StatusUpdate(newStatus, remarks, actor));
    }

    public void setAssignedDepartment(String departmentName) {
        this.assignedDepartment = departmentName;
    }

    public String getComplaintId() { return complaintId; }
    public Citizen getCitizen() { return citizen; }
    public Category getCategory() { return category; }
    public Severity getSeverity() { return severity; }
    public int getAffectedCount() { return affectedCount; }
    public long getArrivalOrder() { return arrivalOrder; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getOpenIssueRef() { return openIssueRef; }
    public ComplaintStatus getStatus() { return status; }
    public String getAssignedDepartment() { return assignedDepartment; }
    public List<StatusUpdate> getHistory() { return Collections.unmodifiableList(history); }

    @Override
    public String toString() {
        return String.format("%-12s | %-12s | %-8s | affected=%-4d | %-12s | %s",
                complaintId, category, severity, affectedCount, status, location);
    }
}
