package com.makkalnagar.service;

import com.makkalnagar.dto.DuplicateCandidateResponse;
import com.makkalnagar.entity.ComplaintEntity;
import com.makkalnagar.entity.DuplicateLinkEntity;
import com.makkalnagar.enums.DuplicateStatus;
import com.makkalnagar.exception.ResourceNotFoundException;
import com.makkalnagar.repository.DuplicateLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DuplicateService {

    private final DuplicateLinkRepository duplicateLinkRepository;

    public DuplicateService(DuplicateLinkRepository duplicateLinkRepository) {
        this.duplicateLinkRepository = duplicateLinkRepository;
    }

    @Transactional(readOnly = true)
    public List<DuplicateCandidateResponse> getDuplicatesForComplaint(Long complaintId) {
        List<DuplicateLinkEntity> links = duplicateLinkRepository.findAllByComplaintId(complaintId);
        return links.stream().map(link -> {
            ComplaintEntity target = link.getSourceComplaint().getId().equals(complaintId) ?
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
            dto.setReviewedBy(link.getReviewedBy());
            dto.setReviewedAt(link.getReviewedAt());
            dto.setCreatedAt(link.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public DuplicateCandidateResponse confirmDuplicate(Long duplicateLinkId, String reviewer) {
        DuplicateLinkEntity link = duplicateLinkRepository.findById(duplicateLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Duplicate link record not found with ID: " + duplicateLinkId));

        link.setStatus(DuplicateStatus.CONFIRMED);
        link.setReviewedBy(reviewer);
        link.setReviewedAt(LocalDateTime.now());
        link = duplicateLinkRepository.save(link);

        return mapSingle(link);
    }

    @Transactional
    public DuplicateCandidateResponse dismissDuplicate(Long duplicateLinkId, String reviewer) {
        DuplicateLinkEntity link = duplicateLinkRepository.findById(duplicateLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Duplicate link record not found with ID: " + duplicateLinkId));

        link.setStatus(DuplicateStatus.DISMISSED);
        link.setReviewedBy(reviewer);
        link.setReviewedAt(LocalDateTime.now());
        link = duplicateLinkRepository.save(link);

        return mapSingle(link);
    }

    private DuplicateCandidateResponse mapSingle(DuplicateLinkEntity link) {
        DuplicateCandidateResponse dto = new DuplicateCandidateResponse();
        dto.setDuplicateLinkId(link.getId());
        dto.setTargetComplaintInternalId(link.getTargetComplaint().getId());
        dto.setTargetComplaintId(link.getTargetComplaint().getComplaintId());
        dto.setCategory(link.getTargetComplaint().getCategory());
        dto.setLocation(link.getTargetComplaint().getLocation());
        dto.setSeverity(link.getTargetComplaint().getSeverity());
        dto.setDescription(link.getTargetComplaint().getDescription());
        dto.setOpenIssueRef(link.getTargetComplaint().getOpenIssueRef());
        dto.setStatus(link.getStatus());
        dto.setStatusDisplayName(link.getStatus().getDescription());
        dto.setReason(link.getReason());
        dto.setReviewedBy(link.getReviewedBy());
        dto.setReviewedAt(link.getReviewedAt());
        dto.setCreatedAt(link.getCreatedAt());
        return dto;
    }
}
