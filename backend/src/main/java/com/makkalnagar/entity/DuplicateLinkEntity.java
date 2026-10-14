package com.makkalnagar.entity;

import com.makkalnagar.enums.DuplicateStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "duplicate_links")
public class DuplicateLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_complaint_id", nullable = false)
    private ComplaintEntity sourceComplaint;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_complaint_id", nullable = false)
    private ComplaintEntity targetComplaint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DuplicateStatus status = DuplicateStatus.FLAGGED;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore = 0.0;

    @Column(name = "similarity_details", columnDefinition = "TEXT")
    private String similarityDetails;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DuplicateLinkEntity() {}

    public DuplicateLinkEntity(ComplaintEntity sourceComplaint, ComplaintEntity targetComplaint, DuplicateStatus status, String reason) {
        this(sourceComplaint, targetComplaint, status, reason, 0.0, null);
    }

    public DuplicateLinkEntity(ComplaintEntity sourceComplaint, ComplaintEntity targetComplaint, DuplicateStatus status,
                               String reason, Double confidenceScore, String similarityDetails) {
        this.sourceComplaint = sourceComplaint;
        this.targetComplaint = targetComplaint;
        this.status = status;
        this.reason = reason;
        this.confidenceScore = confidenceScore != null ? confidenceScore : 0.0;
        this.similarityDetails = similarityDetails;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ComplaintEntity getSourceComplaint() { return sourceComplaint; }
    public void setSourceComplaint(ComplaintEntity sourceComplaint) { this.sourceComplaint = sourceComplaint; }

    public ComplaintEntity getTargetComplaint() { return targetComplaint; }
    public void setTargetComplaint(ComplaintEntity targetComplaint) { this.targetComplaint = targetComplaint; }

    public DuplicateStatus getStatus() { return status; }
    public void setStatus(DuplicateStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getSimilarityDetails() { return similarityDetails; }
    public void setSimilarityDetails(String similarityDetails) { this.similarityDetails = similarityDetails; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
