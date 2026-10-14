package com.makkalnagar.controller;

import com.makkalnagar.dto.ComplaintRegistrationRequest;
import com.makkalnagar.dto.ComplaintResponse;
import com.makkalnagar.dto.PublicTrackResponse;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.repository.ComplaintRepository;
import com.makkalnagar.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicComplaintController {

    private final ComplaintService complaintService;
    private final ComplaintRepository complaintRepository;

    public PublicComplaintController(ComplaintService complaintService, ComplaintRepository complaintRepository) {
        this.complaintService = complaintService;
        this.complaintRepository = complaintRepository;
    }

    @PostMapping("/complaints")
    public ResponseEntity<ComplaintResponse> registerComplaint(
            @Valid @RequestBody ComplaintRegistrationRequest request) {
        ComplaintResponse response = complaintService.registerComplaint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/complaints/{complaintId}/track")
    public ResponseEntity<PublicTrackResponse> trackComplaint(@PathVariable String complaintId) {
        PublicTrackResponse response = complaintService.getPublicTracking(complaintId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPublicStats() {
        long total = complaintRepository.count();
        long resolved = complaintRepository.countByStatus(ComplaintStatus.RESOLVED);
        long active = complaintRepository.countByStatus(ComplaintStatus.ASSIGNED) +
                      complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS);
        long inQueue = complaintRepository.countByStatus(ComplaintStatus.REGISTERED);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalComplaints", total);
        stats.put("resolvedComplaints", resolved);
        stats.put("activeProcessing", active);
        stats.put("waitingQueue", inQueue);
        stats.put("resolutionRate", total > 0 ? Math.round(((double) resolved / total * 100.0) * 10.0) / 10.0 : 0.0);

        return ResponseEntity.ok(stats);
    }
}
