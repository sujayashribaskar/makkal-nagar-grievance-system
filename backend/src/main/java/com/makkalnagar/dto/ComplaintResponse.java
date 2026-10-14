package com.makkalnagar.dto;

import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.enums.Severity;

import java.time.LocalDateTime;
import java.util.List;

public class ComplaintResponse {

    private Long id;
    private String complaintId;
    private String citizenName;
    private String citizenReference;
    private String phone;
    private Category category;
    private String categoryDisplayName;
    private String description;
    private String location;
    private Severity severity;
    private String severityDisplayName;
    private Integer affectedCount;
    private Long arrivalOrder;
    private ComplaintStatus status;
    private String statusDisplayName;
    private Long departmentId;
    private String departmentName;
    private String openIssueRef;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String priorityRankExplanation;
    private Boolean isDuplicateCandidate;

    private List<StatusUpdateDto> statusHistory;
    private List<AssignmentDto> assignmentHistory;

    public ComplaintResponse() {}

    public static class StatusUpdateDto {
        private String previousStatus;
        private String newStatus;
        private String changedBy;
        private String remarks;
        private LocalDateTime timestamp;

        public StatusUpdateDto(String previousStatus, String newStatus, String changedBy, String remarks, LocalDateTime timestamp) {
            this.previousStatus = previousStatus;
            this.newStatus = newStatus;
            this.changedBy = changedBy;
            this.remarks = remarks;
            this.timestamp = timestamp;
        }

        public String getPreviousStatus() { return previousStatus; }
        public String getNewStatus() { return newStatus; }
        public String getChangedBy() { return changedBy; }
        public String getRemarks() { return remarks; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class AssignmentDto {
        private String departmentName;
        private String assignedBy;
        private LocalDateTime assignedAt;
        private String notes;

        public AssignmentDto(String departmentName, String assignedBy, LocalDateTime assignedAt, String notes) {
            this.departmentName = departmentName;
            this.assignedBy = assignedBy;
            this.assignedAt = assignedAt;
            this.notes = notes;
        }

        public String getDepartmentName() { return departmentName; }
        public String getAssignedBy() { return assignedBy; }
        public LocalDateTime getAssignedAt() { return assignedAt; }
        public String getNotes() { return notes; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }

    public String getCitizenReference() { return citizenReference; }
    public void setCitizenReference(String citizenReference) { this.citizenReference = citizenReference; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getCategoryDisplayName() { return categoryDisplayName; }
    public void setCategoryDisplayName(String categoryDisplayName) { this.categoryDisplayName = categoryDisplayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getSeverityDisplayName() { return severityDisplayName; }
    public void setSeverityDisplayName(String severityDisplayName) { this.severityDisplayName = severityDisplayName; }

    public Integer getAffectedCount() { return affectedCount; }
    public void setAffectedCount(Integer affectedCount) { this.affectedCount = affectedCount; }

    public Long getArrivalOrder() { return arrivalOrder; }
    public void setArrivalOrder(Long arrivalOrder) { this.arrivalOrder = arrivalOrder; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String getStatusDisplayName() { return statusDisplayName; }
    public void setStatusDisplayName(String statusDisplayName) { this.statusDisplayName = statusDisplayName; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getOpenIssueRef() { return openIssueRef; }
    public void setOpenIssueRef(String openIssueRef) { this.openIssueRef = openIssueRef; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getPriorityRankExplanation() { return priorityRankExplanation; }
    public void setPriorityRankExplanation(String priorityRankExplanation) { this.priorityRankExplanation = priorityRankExplanation; }

    public Boolean getIsDuplicateCandidate() { return isDuplicateCandidate; }
    public void setIsDuplicateCandidate(Boolean isDuplicateCandidate) { this.isDuplicateCandidate = isDuplicateCandidate; }

    public List<StatusUpdateDto> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<StatusUpdateDto> statusHistory) { this.statusHistory = statusHistory; }

    public List<AssignmentDto> getAssignmentHistory() { return assignmentHistory; }
    public void setAssignmentHistory(List<AssignmentDto> assignmentHistory) { this.assignmentHistory = assignmentHistory; }
}
