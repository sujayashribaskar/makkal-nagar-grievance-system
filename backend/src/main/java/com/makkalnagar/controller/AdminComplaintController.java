package com.makkalnagar.controller;

import com.makkalnagar.domain.Grievance;
import com.makkalnagar.dto.*;
import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.enums.Severity;
import com.makkalnagar.service.ComplaintService;
import com.makkalnagar.service.DepartmentQueueService;
import com.makkalnagar.service.DuplicateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/complaints")
public class AdminComplaintController {

    private final ComplaintService complaintService;
    private final DuplicateService duplicateService;
    private final DepartmentQueueService departmentQueueService;

    public AdminComplaintController(ComplaintService complaintService,
                                    DuplicateService duplicateService,
                                    DepartmentQueueService departmentQueueService) {
        this.complaintService = complaintService;
        this.duplicateService = duplicateService;
        this.departmentQueueService = departmentQueueService;
    }

    @GetMapping
    public ResponseEntity<Page<ComplaintResponse>> getComplaints(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort sort = direction.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ComplaintResponse> complaints = complaintService.getFilteredComplaints(
                category, location, severity, status, departmentId, search, pageable);

        return ResponseEntity.ok(complaints);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaintById(@PathVariable Long id) {
        ComplaintResponse response = complaintService.getComplaintById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "admin";
        ComplaintResponse response = complaintService.updateStatus(id, request, actor);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ComplaintResponse> assignDepartment(
            @PathVariable Long id,
            @Valid @RequestBody AssignRequest request,
            Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "admin";
        ComplaintResponse response = complaintService.reassignDepartment(id, request, actor);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reassign")
    public ResponseEntity<ComplaintResponse> reassignDepartment(
            @PathVariable Long id,
            @Valid @RequestBody AssignRequest request,
            Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "admin";
        ComplaintResponse response = complaintService.reassignDepartment(id, request, actor);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/duplicates")
    public ResponseEntity<List<DuplicateCandidateResponse>> getDuplicates(@PathVariable Long id) {
        List<DuplicateCandidateResponse> candidates = duplicateService.getDuplicatesForComplaint(id);
        return ResponseEntity.ok(candidates);
    }

    @PostMapping("/{id}/duplicates/{duplicateId}/confirm")
    public ResponseEntity<DuplicateCandidateResponse> confirmDuplicate(
            @PathVariable Long id,
            @PathVariable Long duplicateId,
            Authentication authentication) {
        String reviewer = authentication != null ? authentication.getName() : "admin";
        DuplicateCandidateResponse response = duplicateService.confirmDuplicate(duplicateId, reviewer);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/duplicates/{duplicateId}/dismiss")
    public ResponseEntity<DuplicateCandidateResponse> dismissDuplicate(
            @PathVariable Long id,
            @PathVariable Long duplicateId,
            Authentication authentication) {
        String reviewer = authentication != null ? authentication.getName() : "admin";
        DuplicateCandidateResponse response = duplicateService.dismissDuplicate(duplicateId, reviewer);
        return ResponseEntity.ok(response);
    }

    /**
     * Inspects the in-memory PriorityQueue for a department in strict ranking order.
     */
    @GetMapping("/waiting/{category}")
    public ResponseEntity<List<String>> getWaitingQueue(@PathVariable Category category) {
        List<Grievance> queue = departmentQueueService.getWaitingQueueSnapshot(category);
        List<String> list = queue.stream()
                .map(g -> String.format("[%s] %s | %s | affected=%d | order=#%d | %s",
                        g.getComplaintId(), g.getCategory(), g.getSeverity(),
                        g.getAffectedCount(), g.getArrivalOrder(), g.getLocation()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}
