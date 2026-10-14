package com.makkalnagar.dto;

import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.DuplicateStatus;
import com.makkalnagar.enums.Severity;

import java.time.LocalDateTime;

public class DuplicateCandidateResponse {

    private Long duplicateLinkId;
    private Long targetComplaintInternalId;
    private String targetComplaintId;
    private Category category;
    private String location;
    private Severity severity;
    private String description;
    private String openIssueRef;
    private DuplicateStatus status;
    private String statusDisplayName;
    private String reason;
    private Double confidenceScore = 0.0;
    private String similarityDetails;
    private boolean confirmedDuplicate = false;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    public DuplicateCandidateResponse() {}

    public Long getDuplicateLinkId() { return duplicateLinkId; }
    public void setDuplicateLinkId(Long duplicateLinkId) { this.duplicateLinkId = duplicateLinkId; }

    public Long getTargetComplaintInternalId() { return targetComplaintInternalId; }
    public void setTargetComplaintInternalId(Long targetComplaintInternalId) { this.targetComplaintInternalId = targetComplaintInternalId; }

    public String getTargetComplaintId() { return targetComplaintId; }
    public void setTargetComplaintId(String targetComplaintId) { this.targetComplaintId = targetComplaintId; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOpenIssueRef() { return openIssueRef; }
    public void setOpenIssueRef(String openIssueRef) { this.openIssueRef = openIssueRef; }

    public DuplicateStatus getStatus() { return status; }
    public void setStatus(DuplicateStatus status) { this.status = status; }

    public String getStatusDisplayName() { return statusDisplayName; }
    public void setStatusDisplayName(String statusDisplayName) { this.statusDisplayName = statusDisplayName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getSimilarityDetails() { return similarityDetails; }
    public void setSimilarityDetails(String similarityDetails) { this.similarityDetails = similarityDetails; }

    public boolean isConfirmedDuplicate() { return confirmedDuplicate; }
    public void setConfirmedDuplicate(boolean confirmedDuplicate) { this.confirmedDuplicate = confirmedDuplicate; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
