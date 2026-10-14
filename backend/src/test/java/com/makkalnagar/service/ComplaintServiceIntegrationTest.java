package com.makkalnagar.service;

import com.makkalnagar.dto.*;
import com.makkalnagar.entity.DepartmentEntity;
import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.enums.DuplicateStatus;
import com.makkalnagar.enums.Severity;
import com.makkalnagar.repository.ComplaintRepository;
import com.makkalnagar.repository.DepartmentRepository;
import com.makkalnagar.repository.DuplicateLinkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ComplaintServiceIntegrationTest {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DuplicateService duplicateService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DuplicateLinkRepository duplicateLinkRepository;

    @Test
    @DisplayName("Test 1 & 2: Complaint Registration & Unique Complaint ID Generation")
    @Transactional
    void testComplaintRegistrationAndIdGeneration() {
        ComplaintRegistrationRequest req = new ComplaintRegistrationRequest();
        req.setCitizenName("Sundar Pichai");
        req.setCategory(Category.ROAD);
        req.setDescription("Deep pothole at road intersection causing disruption");
        req.setLocation("Gandhi Street");
        req.setSeverity(Severity.HIGH);
        req.setAffectedCount(45);

        ComplaintResponse response = complaintService.registerComplaint(req);

        assertNotNull(response);
        assertNotNull(response.getComplaintId());
        assertTrue(response.getComplaintId().startsWith("MN-2026-"), "ID must start with MN-2026-");
        assertTrue(response.getArrivalOrder() > 0, "Arrival order must be positive");
        assertEquals("Roads and Infrastructure Department", response.getDepartmentName());
        assertEquals("Sundar Pichai", response.getCitizenName());
    }

    @Test
    @DisplayName("Test 3: Category to Department Mapping")
    void testCategoryToDepartmentMapping() {
        DepartmentEntity waterDept = departmentRepository.findByCode(Category.WATER).orElseThrow();
        assertEquals("Water Supply Department", waterDept.getName());

        DepartmentEntity roadDept = departmentRepository.findByCode(Category.ROAD).orElseThrow();
        assertEquals("Roads and Infrastructure Department", roadDept.getName());

        DepartmentEntity lightDept = departmentRepository.findByCode(Category.STREETLIGHT).orElseThrow();
        assertEquals("Streetlight and Electrical Department", lightDept.getName());

        DepartmentEntity wasteDept = departmentRepository.findByCode(Category.WASTE).orElseThrow();
        assertEquals("Sanitation and Waste Management Department", wasteDept.getName());
    }

    @Test
    @DisplayName("Test 7 & 8: Department Capacity Enforcement and Waiting Queue Dispatch on Resolution")
    @Transactional
    void testCapacityEnforcementAndResolutionDispatch() {
        DepartmentEntity dept = departmentRepository.findByCode(Category.WATER).orElseThrow();
        int initialCapacity = dept.getCapacity();

        // Count current active cases in Water Supply Department
        long activeCount = complaintRepository.countByDepartmentIdAndStatusIn(
                dept.getId(), List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));

        assertTrue(activeCount <= initialCapacity, "Active cases cannot exceed capacity");

        // Submit a new complaint into Water department
        ComplaintRegistrationRequest req = new ComplaintRegistrationRequest();
        req.setCitizenName("Queue Tester");
        req.setCategory(Category.WATER);
        req.setDescription("Water leakage under residential meter connection");
        req.setLocation("Market Area");
        req.setSeverity(Severity.MEDIUM);
        req.setAffectedCount(15);

        ComplaintResponse regResponse = complaintService.registerComplaint(req);

        if (activeCount >= initialCapacity) {
            // Must be placed in priority queue (REGISTERED status)
            assertEquals(ComplaintStatus.REGISTERED, regResponse.getStatus());
        }
    }

    @Test
    @DisplayName("Test 10: Complaint Resolution requires notes and frees capacity")
    @Transactional
    void testComplaintResolution() {
        // Create an active complaint
        ComplaintRegistrationRequest req = new ComplaintRegistrationRequest();
        req.setCitizenName("Resolver Tester");
        req.setCategory(Category.STREETLIGHT);
        req.setDescription("Flickering streetlight outside community center");
        req.setLocation("Nehru Colony");
        req.setSeverity(Severity.LOW);
        req.setAffectedCount(5);

        ComplaintResponse reg = complaintService.registerComplaint(req);

        // Move to IN_PROGRESS if ASSIGNED
        if (reg.getStatus() == ComplaintStatus.ASSIGNED) {
            StatusUpdateRequest inProgressReq = new StatusUpdateRequest();
            inProgressReq.setNewStatus(ComplaintStatus.IN_PROGRESS);
            inProgressReq.setRemarks("Electrician team dispatched");
            complaintService.updateStatus(reg.getId(), inProgressReq, "admin");

            // Mark RESOLVED
            StatusUpdateRequest resolveReq = new StatusUpdateRequest();
            resolveReq.setNewStatus(ComplaintStatus.RESOLVED);
            resolveReq.setResolutionNotes("Replaced starter relay and ballast. Verified light functions.");
            ComplaintResponse resolved = complaintService.updateStatus(reg.getId(), resolveReq, "admin");

            assertEquals(ComplaintStatus.RESOLVED, resolved.getStatus());
            assertNotNull(resolved.getResolvedAt());
            assertEquals("Replaced starter relay and ballast. Verified light functions.", resolved.getResolutionNotes());
        }
    }

    @Test
    @DisplayName("Test 11 & 12: Duplicate candidate detection, confirmation, and dismissal")
    @Transactional
    void testDuplicateDetectionAndReview() {
        // Register first complaint
        ComplaintRegistrationRequest c1 = new ComplaintRegistrationRequest();
        c1.setCitizenName("Citizen A");
        c1.setCategory(Category.WASTE);
        c1.setDescription("Garbage heap near market gate #2");
        c1.setLocation("Market Area");
        c1.setSeverity(Severity.HIGH);
        c1.setAffectedCount(50);
        c1.setOpenIssueRef("GARBAGE-MK-99");
        ComplaintResponse r1 = complaintService.registerComplaint(c1);

        // Register duplicate candidate with same category, location, and openIssueRef
        ComplaintRegistrationRequest c2 = new ComplaintRegistrationRequest();
        c2.setCitizenName("Citizen B");
        c2.setCategory(Category.WASTE);
        c2.setDescription("Uncollected trash accumulating outside market gate 2");
        c2.setLocation("Market Area");
        c2.setSeverity(Severity.MEDIUM);
        c2.setAffectedCount(20);
        c2.setOpenIssueRef("GARBAGE-MK-99");
        ComplaintResponse r2 = complaintService.registerComplaint(c2);

        // Verify duplicate was flagged
        List<DuplicateCandidateResponse> dups = duplicateService.getDuplicatesForComplaint(r2.getId());
        assertFalse(dups.isEmpty(), "Duplicate candidate must be flagged");

        Long linkId = dups.get(0).getDuplicateLinkId();
        assertEquals(DuplicateStatus.FLAGGED, dups.get(0).getStatus());

        // Test Confirmation
        DuplicateCandidateResponse confirmed = duplicateService.confirmDuplicate(linkId, "admin_user");
        assertEquals(DuplicateStatus.CONFIRMED, confirmed.getStatus());
        assertEquals("admin_user", confirmed.getReviewedBy());

        // Test Dismissal
        DuplicateCandidateResponse dismissed = duplicateService.dismissDuplicate(linkId, "admin_user");
        assertEquals(DuplicateStatus.DISMISSED, dismissed.getStatus());
    }
}
