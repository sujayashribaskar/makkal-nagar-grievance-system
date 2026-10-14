package com.makkalnagar.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public DTO for citizen tracking.
 * Strictly sanitizes private citizen data and internal administrative notes.
 */
public class PublicTrackResponse {

    private String complaintId;
    private String category;
    private String location;
    private String status;
    private String statusDisplayName;
    private String assignedDepartment;
    private LocalDateTime registrationDate;
    private String resolutionNotes;
    private LocalDateTime resolutionDate;
    private List<PublicStatusEvent> statusTimeline;

    public PublicTrackResponse() {}

    public static class PublicStatusEvent {
        private String status;
        private String statusDisplayName;
        private LocalDateTime timestamp;
        private String publicRemark;

        public PublicStatusEvent(String status, String statusDisplayName, LocalDateTime timestamp, String publicRemark) {
            this.status = status;
            this.statusDisplayName = statusDisplayName;
            this.timestamp = timestamp;
            this.publicRemark = publicRemark;
        }

        public String getStatus() { return status; }
        public String getStatusDisplayName() { return statusDisplayName; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getPublicRemark() { return publicRemark; }
    }

    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusDisplayName() { return statusDisplayName; }
    public void setStatusDisplayName(String statusDisplayName) { this.statusDisplayName = statusDisplayName; }

    public String getAssignedDepartment() { return assignedDepartment; }
    public void setAssignedDepartment(String assignedDepartment) { this.assignedDepartment = assignedDepartment; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public LocalDateTime getResolutionDate() { return resolutionDate; }
    public void setResolutionDate(LocalDateTime resolutionDate) { this.resolutionDate = resolutionDate; }

    public List<PublicStatusEvent> getStatusTimeline() { return statusTimeline; }
    public void setStatusTimeline(List<PublicStatusEvent> statusTimeline) { this.statusTimeline = statusTimeline; }
}
