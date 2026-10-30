package com.makkalnagar.service;

import com.makkalnagar.dto.DuplicateCandidateResponse;
import com.makkalnagar.entity.ComplaintEntity;
import com.makkalnagar.entity.DuplicateLinkEntity;
import com.makkalnagar.enums.DuplicateStatus;
import com.makkalnagar.exception.ResourceNotFoundException;
import com.makkalnagar.repository.DuplicateLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class DuplicateService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateService.class);

    private final DuplicateLinkRepository duplicateLinkRepository;
    private final DuplicateDetectionEngine duplicateDetectionEngine;

    public DuplicateService(DuplicateLinkRepository duplicateLinkRepository,
                            DuplicateDetectionEngine duplicateDetectionEngine) {
        this.duplicateLinkRepository = duplicateLinkRepository;
        this.duplicateDetectionEngine = duplicateDetectionEngine;
    }

    public record DuplicateMatch(ComplaintEntity existingComplaint, DuplicateDetectionEngine.DetectionResult result) {}

    /**
     * Checks if a candidate complaint matches any existing open complaints as a duplicate.
     * Returns the duplicate match if confidence score meets the duplicate threshold (>= 0.60).
     */
    public Optional<DuplicateMatch> findDuplicateMatch(ComplaintEntity candidate, List<ComplaintEntity> candidates) {
        for (ComplaintEntity existing : candidates) {
            if (candidate.getId() != null && existing.getId() != null && existing.getId().equals(candidate.getId())) {
                continue;
            }

            DuplicateDetectionEngine.DetectionResult result = duplicateDetectionEngine.evaluate(candidate, existing);
            if (result.isDuplicate() && result.getConfidenceScore() >= DuplicateDetectionEngine.DUPLICATE_THRESHOLD) {
                return Optional.of(new DuplicateMatch(existing, result));
            }
        }
        return Optional.empty();
    }

    /**
     * Automatically evaluates a new complaint against existing complaints.
     * Flags and records duplicate links with computed confidence scores and detailed matching reasons.
     * Prevents repeated records when duplicate pairs already exist.
     */
    @Transactional
    public List<DuplicateLinkEntity> evaluateAndPersistDuplicates(ComplaintEntity newComplaint, List<ComplaintEntity> candidates) {
        List<DuplicateLinkEntity> createdOrUpdatedLinks = new ArrayList<>();

        for (ComplaintEntity existing : candidates) {
            if (existing.getId().equals(newComplaint.getId())) continue;

            DuplicateDetectionEngine.DetectionResult result = duplicateDetectionEngine.evaluate(newComplaint, existing);

            if (result.isDuplicate()) {
                Optional<DuplicateLinkEntity> existingLinkOpt = duplicateLinkRepository.findPair(newComplaint.getId(), existing.getId());

                if (existingLinkOpt.isPresent()) {
                    DuplicateLinkEntity existingLink = existingLinkOpt.get();
                    existingLink.setConfidenceScore(result.getConfidenceScore());
                    existingLink.setStatus(result.getStatus());
                    existingLink.setReason(result.getReason());
                    existingLink.setSimilarityDetails(result.getSimilarityDetails());
                    DuplicateLinkEntity saved = duplicateLinkRepository.save(existingLink);
                    createdOrUpdatedLinks.add(saved);
                    log.info("Updated existing duplicate relationship between {} and {} with confidence {}: {}",
                            newComplaint.getComplaintId(), existing.getComplaintId(), result.getConfidenceScore(), result.getReason());
                } else {
                    DuplicateLinkEntity link = new DuplicateLinkEntity(
                            newComplaint,
                            existing,
                            result.getStatus(),
                            result.getReason(),
                            result.getConfidenceScore(),
                            result.getSimilarityDetails()
                    );
                    DuplicateLinkEntity saved = duplicateLinkRepository.save(link);
                    createdOrUpdatedLinks.add(saved);
                    log.info("Automatically flagged duplicate candidate between {} and {} (Confidence: {}%): {}",
                            newComplaint.getComplaintId(), existing.getComplaintId(),
                            Math.round(result.getConfidenceScore() * 100), result.getReason());
                }
            }
        }

        return createdOrUpdatedLinks;
    }

    @Transactional(readOnly = true)
    public List<DuplicateCandidateResponse> getDuplicatesForComplaint(Long complaintId) {
        List<DuplicateLinkEntity> links = duplicateLinkRepository.findAllByComplaintId(complaintId);
        return links.stream().map(link -> mapSingle(link, complaintId)).collect(Collectors.toList());
    }

    @Transactional
    public DuplicateCandidateResponse confirmDuplicate(Long duplicateLinkId, String reviewer) {
        DuplicateLinkEntity link = duplicateLinkRepository.findById(duplicateLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Duplicate link record not found with ID: " + duplicateLinkId));

        link.setStatus(DuplicateStatus.CONFIRMED);
        link.setReviewedBy(reviewer);
        link.setReviewedAt(LocalDateTime.now());
        link = duplicateLinkRepository.save(link);

        return mapSingle(link, link.getSourceComplaint().getId());
    }

    @Transactional
    public DuplicateCandidateResponse dismissDuplicate(Long duplicateLinkId, String reviewer) {
        DuplicateLinkEntity link = duplicateLinkRepository.findById(duplicateLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Duplicate link record not found with ID: " + duplicateLinkId));

        link.setStatus(DuplicateStatus.DISMISSED);
        link.setReviewedBy(reviewer);
        link.setReviewedAt(LocalDateTime.now());
        link = duplicateLinkRepository.save(link);

        return mapSingle(link, link.getSourceComplaint().getId());
    }

    private DuplicateCandidateResponse mapSingle(DuplicateLinkEntity link, Long baseComplaintId) {
        ComplaintEntity target = link.getSourceComplaint().getId().equals(baseComplaintId) ?
                link.getTargetComplaint() : link.getSourceComplaint();

        DuplicateCandidateResponse dto = new DuplicateCandidateResponse();
        dto.setDuplicateLinkId(link.getId());
        dto.setTargetComplaintInternalId(target.getId());
        dto.setTargetComplaintId(target.getComplaintId());
        dto.setCategory(target.getCategory());
        dto.setLocation(target.getLocation());
        dto.setSeverity(target.getSeverity());
        dto.setDescription(target.getDescription());
        dto.setOpenIssueRef(target.getOpenIssueRef());
        dto.setStatus(link.getStatus());
        dto.setStatusDisplayName(link.getStatus().getDescription());
        dto.setReason(link.getReason());
        dto.setConfidenceScore(link.getConfidenceScore());
        dto.setSimilarityDetails(link.getSimilarityDetails());
        dto.setConfirmedDuplicate(link.getStatus() == DuplicateStatus.CONFIRMED ||
                link.getStatus() == DuplicateStatus.CONFIRMED_DUPLICATE ||
                (link.getConfidenceScore() != null && link.getConfidenceScore() >= DuplicateDetectionEngine.HIGH_CONFIDENCE_THRESHOLD));
        dto.setReviewedBy(link.getReviewedBy());
        dto.setReviewedAt(link.getReviewedAt());
        dto.setCreatedAt(link.getCreatedAt());
        return dto;
    }
}
