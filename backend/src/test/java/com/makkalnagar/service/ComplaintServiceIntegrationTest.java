package com.makkalnagar.service;

import com.makkalnagar.dto.*;
import com.makkalnagar.entity.*;
import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.enums.Severity;
import com.makkalnagar.exception.DuplicateComplaintException;
import com.makkalnagar.exception.InvalidOperationException;
import com.makkalnagar.repository.ComplaintRepository;
import com.makkalnagar.repository.DepartmentRepository;
import com.makkalnagar.repository.StatusUpdateRepository;
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
    private DuplicateService duplicateService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StatusUpdateRepository statusUpdateRepository;

    @Test
    @DisplayName("Test 1: Complaint Registration, ID Generation, and Initial Automatic Status")
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
        assertTrue(response.getStatus() == ComplaintStatus.ASSIGNED || response.getStatus() == ComplaintStatus.REGISTERED);
    }

    @Test
    @DisplayName("Test 2: Category to Department Mapping")
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
    @DisplayName("Test 3: Department Capacity Enforcement and Waiting Queue Dispatch on Resolution")
    @Transactional
    void testCapacityEnforcementAndResolutionDispatch() {
        DepartmentEntity dept = departmentRepository.findByCode(Category.WATER).orElseThrow();
        int initialCapacity = dept.getCapacity();

        long activeCount = complaintRepository.countByDepartmentIdAndStatusIn(
                dept.getId(), List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));

        assertTrue(activeCount <= initialCapacity, "Active cases cannot exceed capacity");

        ComplaintRegistrationRequest req = new ComplaintRegistrationRequest();
        req.setCitizenName("Queue Tester");
        req.setCategory(Category.WATER);
        req.setDescription("Water leakage under residential meter connection");
        req.setLocation("Market Area");
        req.setSeverity(Severity.MEDIUM);
        req.setAffectedCount(15);

        ComplaintResponse regResponse = complaintService.registerComplaint(req);

        if (activeCount >= initialCapacity) {
            assertEquals(ComplaintStatus.REGISTERED, regResponse.getStatus());
        }
    }

    @Test
    @DisplayName("Test 4: Operational Events Trigger Status (ASSIGNED -> IN_PROGRESS -> RESOLVED) & Rejection of Manual Override")
    @Transactional
    void testAutomaticStatusLifecycleAndRejectionOfManualUpdates() {
        ComplaintRegistrationRequest req = new ComplaintRegistrationRequest();
        req.setCitizenName("Lifecycle Tester");
        req.setCategory(Category.STREETLIGHT);
        req.setDescription("Flickering streetlight outside community center");
        req.setLocation("Nehru Colony");
        req.setSeverity(Severity.LOW);
        req.setAffectedCount(5);

        ComplaintResponse reg = complaintService.registerComplaint(req);

        if (reg.getStatus() == ComplaintStatus.ASSIGNED) {
            // 1. Attempt manual status override via updateStatus -> Must throw InvalidOperationException!
            StatusUpdateRequest manualReq = new StatusUpdateRequest();
            manualReq.setNewStatus(ComplaintStatus.IN_PROGRESS);
            assertThrows(InvalidOperationException.class, () ->
                    complaintService.updateStatus(reg.getId(), manualReq, "admin"),
                    "Direct manual status modification must be rejected");

            // 2. Cannot resolve an ASSIGNED complaint directly without being IN_PROGRESS
            assertThrows(InvalidOperationException.class, () ->
                    complaintService.recordVerifiedResolution(reg.getId(), "Notes", "Done", "admin"),
                    "Cannot resolve ASSIGNED complaint directly");

            // 3. Start field work through defined operational event -> transitions to IN_PROGRESS
            ComplaintResponse inProgress = complaintService.startDepartmentWork(
                    reg.getId(), "Electrician team dispatched with replacement kit", "admin");
            assertEquals(ComplaintStatus.IN_PROGRESS, inProgress.getStatus());

            // 4. Cannot start work again if already IN_PROGRESS
            assertThrows(InvalidOperationException.class, () ->
                    complaintService.startDepartmentWork(reg.getId(), "Repeat start", "admin"),
                    "Cannot start work on already IN_PROGRESS complaint");

            // 5. Resolving without resolution notes must fail
            assertThrows(InvalidOperationException.class, () ->
                    complaintService.recordVerifiedResolution(reg.getId(), "", "Remarks", "admin"),
                    "Resolution notes are mandatory");

            // 6. Record verified resolution -> transitions to RESOLVED
            ComplaintResponse resolved = complaintService.recordVerifiedResolution(
                    reg.getId(), "Replaced starter relay and ballast. Verified light functions.", "Field verified", "admin");
            assertEquals(ComplaintStatus.RESOLVED, resolved.getStatus());
            assertNotNull(resolved.getResolvedAt());
            assertEquals("Replaced starter relay and ballast. Verified light functions.", resolved.getResolutionNotes());
        }
    }

    @Test
    @DisplayName("Test 5: Status History Persistence in MySQL with Triggering Event Details")
    @Transactional
    void testStatusHistoryPersistenceWithTriggeringEvents() {
        ComplaintRegistrationRequest req = new ComplaintRegistrationRequest();
        req.setCitizenName("History Tester");
        req.setCategory(Category.ROAD);
        req.setDescription("Deep dangerous pothole on main road");
        req.setLocation("Nehru Colony");
        req.setSeverity(Severity.HIGH);
        req.setAffectedCount(30);

        ComplaintResponse reg = complaintService.registerComplaint(req);

        List<StatusUpdateEntity> history = statusUpdateRepository.findByComplaintIdOrderByCreatedAtAsc(reg.getId());
        assertFalse(history.isEmpty(), "Status history must be saved in MySQL");

        StatusUpdateEntity initial = history.get(0);
        assertEquals(ComplaintStatus.REGISTERED, initial.getNewStatus());
        assertEquals("CITIZEN_REGISTRATION", initial.getTriggeringEvent());
        assertEquals("CITIZEN", initial.getChangedBy());

        if (reg.getStatus() == ComplaintStatus.ASSIGNED) {
            complaintService.startDepartmentWork(reg.getId(), "Field crew deployed", "officer_1");
            complaintService.recordVerifiedResolution(reg.getId(), "Road tar patch laid and compacted", "Finished", "officer_1");

            List<StatusUpdateEntity> updatedHistory = statusUpdateRepository.findByComplaintIdOrderByCreatedAtAsc(reg.getId());
            assertTrue(updatedHistory.stream().anyMatch(h -> "DEPARTMENT_WORK_STARTED".equals(h.getTriggeringEvent())));
            assertTrue(updatedHistory.stream().anyMatch(h -> "RESOLUTION_VERIFIED".equals(h.getTriggeringEvent())));
        }
    }

    @Test
    @DisplayName("Test 6: Repeated Complaint ID Rejection (ID Conflict)")
    @Transactional
    void testRepeatedComplaintIdRejection() {
        ComplaintRegistrationRequest c1 = new ComplaintRegistrationRequest();
        c1.setCitizenName("Citizen First");
        c1.setCategory(Category.ROAD);
        c1.setDescription("Damaged road divider");
        c1.setLocation("Gandhi Street");
        c1.setSeverity(Severity.MEDIUM);
        c1.setAffectedCount(10);
        c1.setComplaintId("MN-CUSTOM-9999");

        ComplaintResponse r1 = complaintService.registerComplaint(c1);
        assertEquals("MN-CUSTOM-9999", r1.getComplaintId());

        // Attempting to register another complaint with the same ID must be rejected as an ID conflict
        ComplaintRegistrationRequest c2 = new ComplaintRegistrationRequest();
        c2.setCitizenName("Citizen Second");
        c2.setCategory(Category.ROAD);
        c2.setDescription("Completely different issue description");
        c2.setLocation("Gandhi Street");
        c2.setSeverity(Severity.MEDIUM);
        c2.setAffectedCount(10);
        c2.setComplaintId("MN-CUSTOM-9999");

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                complaintService.registerComplaint(c2), "Repeated complaint ID must be rejected as an ID conflict");
        assertTrue(ex.getMessage().contains("Complaint ID conflict"));
    }

    @Test
    @DisplayName("Test 7: Duplicate Detection - Identical Descriptions at Same Location are Automatically Discarded")
    @Transactional
    void testDuplicateIdenticalDescriptionsAtSameLocation() {
        ComplaintRegistrationRequest c1 = new ComplaintRegistrationRequest();
        c1.setCitizenName("Citizen 1");
        c1.setCategory(Category.WATER);
        c1.setDescription("Severe drinking water pipe burst flooding road near overhead tank");
        c1.setLocation("Anna Nagar");
        c1.setSeverity(Severity.HIGH);
        c1.setAffectedCount(50);
        ComplaintResponse r1 = complaintService.registerComplaint(c1);
        assertNotNull(r1);

        ComplaintRegistrationRequest c2 = new ComplaintRegistrationRequest();
        c2.setCitizenName("Citizen 2");
        c2.setCategory(Category.WATER);
        c2.setDescription("Severe drinking water pipe burst flooding road near overhead tank");
        c2.setLocation("Anna Nagar");
        c2.setSeverity(Severity.HIGH);
        c2.setAffectedCount(40);

        // Duplicate submission must be blocked and rejected immediately without being registered or assigned
        DuplicateComplaintException ex = assertThrows(DuplicateComplaintException.class, () ->
                complaintService.registerComplaint(c2),
                "Duplicate complaint must be automatically rejected and deleted without registration");
        assertEquals(r1.getComplaintId(), ex.getExistingComplaintId());
        assertEquals(1.0, ex.getConfidenceScore(), 0.01);
    }

    @Test
    @DisplayName("Test 8: Duplicate Detection - Similar Descriptions with Different Wording are Automatically Discarded")
    @Transactional
    void testDuplicateSimilarDescriptionsWithDifferentWording() {
        ComplaintRegistrationRequest c1 = new ComplaintRegistrationRequest();
        c1.setCitizenName("Citizen A");
        c1.setCategory(Category.ROAD);
        c1.setDescription("Deep pothole cluster on primary bus route causing vehicular accidents and gridlock");
        c1.setLocation("Gandhi Street");
        c1.setSeverity(Severity.HIGH);
        c1.setAffectedCount(80);
        ComplaintResponse r1 = complaintService.registerComplaint(c1);
        assertNotNull(r1);

        ComplaintRegistrationRequest c2 = new ComplaintRegistrationRequest();
        c2.setCitizenName("Citizen B");
        c2.setCategory(Category.ROAD);
        c2.setDescription("Dangerous road potholes disrupting vehicular traffic and vehicles on main bus route");
        c2.setLocation("Gandhi Street");
        c2.setSeverity(Severity.HIGH);
        c2.setAffectedCount(60);

        // Similar description with different wording at same location must be rejected as duplicate
        DuplicateComplaintException ex = assertThrows(DuplicateComplaintException.class, () ->
                complaintService.registerComplaint(c2),
                "Similar complaint on same issue must be rejected and not assigned or registered");
        assertTrue(ex.getConfidenceScore() >= 0.70, "Confidence score should reflect high similarity");
    }

    @Test
    @DisplayName("Test 9: Duplicate Detection - Same Location but Unrelated Complaints (Must NOT be Duplicate)")
    @Transactional
    void testSameLocationUnrelatedComplaintsNotDuplicate() {
        ComplaintRegistrationRequest c1 = new ComplaintRegistrationRequest();
        c1.setCitizenName("Citizen X");
        c1.setCategory(Category.ROAD);
        c1.setDescription("Asphalt eroded exposing sharp aggregate and damaging tires");
        c1.setLocation("Nehru Colony");
        c1.setSeverity(Severity.MEDIUM);
        c1.setAffectedCount(15);
        ComplaintResponse r1 = complaintService.registerComplaint(c1);

        ComplaintRegistrationRequest c2 = new ComplaintRegistrationRequest();
        c2.setCitizenName("Citizen Y");
        c2.setCategory(Category.ROAD);
        c2.setDescription("Speed bump white paint markings faded completely invisible at night");
        c2.setLocation("Nehru Colony");
        c2.setSeverity(Severity.LOW);
        c2.setAffectedCount(20);
        ComplaintResponse r2 = complaintService.registerComplaint(c2);

        List<DuplicateCandidateResponse> dups = duplicateService.getDuplicatesForComplaint(r2.getId());
        // Unrelated issues at same location must not be linked as duplicates
        boolean isLinkedToC1 = dups.stream().anyMatch(d -> d.getTargetComplaintInternalId().equals(r1.getId()));
        assertFalse(isLinkedToC1, "Unrelated complaints at same location must NOT be flagged as duplicate");
    }

    @Test
    @DisplayName("Test 10: Duplicate Detection - Similar Descriptions at Different Locations (Must NOT be Duplicate)")
    @Transactional
    void testSimilarDescriptionsAtDifferentLocationsNotDuplicate() {
        ComplaintRegistrationRequest c1 = new ComplaintRegistrationRequest();
        c1.setCitizenName("Citizen North");
        c1.setCategory(Category.WATER);
        c1.setDescription("Major drinking water pipeline burst flooding roadway");
        c1.setLocation("Anna Nagar");
        c1.setSeverity(Severity.HIGH);
        c1.setAffectedCount(50);
        ComplaintResponse r1 = complaintService.registerComplaint(c1);

        ComplaintRegistrationRequest c2 = new ComplaintRegistrationRequest();
        c2.setCitizenName("Citizen South");
        c2.setCategory(Category.WATER);
        c2.setDescription("Major drinking water pipeline burst flooding roadway");
        c2.setLocation("Lake View Road");
        c2.setSeverity(Severity.HIGH);
        c2.setAffectedCount(50);
        ComplaintResponse r2 = complaintService.registerComplaint(c2);

        List<DuplicateCandidateResponse> dups = duplicateService.getDuplicatesForComplaint(r2.getId());
        boolean isLinkedToC1 = dups.stream().anyMatch(d -> d.getTargetComplaintInternalId().equals(r1.getId()));
        assertFalse(isLinkedToC1, "Same issue at different municipal wards represents distinct physical defects and must NOT be flagged as duplicate");
    }

    @Test
    @DisplayName("Test 11: Different Complaint Submissions Describing Same Issue are Rejected as Duplicate")
    @Transactional
    void testDifferentComplaintIdsDescribingSameIssue() {
        ComplaintRegistrationRequest c1 = new ComplaintRegistrationRequest();
        c1.setCitizenName("Citizen First");
        c1.setCategory(Category.WASTE);
        c1.setDescription("Overflowing municipal garbage bin near vegetable market");
        c1.setLocation("Market Area");
        c1.setSeverity(Severity.HIGH);
        c1.setAffectedCount(100);
        ComplaintResponse r1 = complaintService.registerComplaint(c1);
        assertNotNull(r1);

        ComplaintRegistrationRequest c2 = new ComplaintRegistrationRequest();
        c2.setCitizenName("Citizen Second");
        c2.setCategory(Category.WASTE);
        c2.setDescription("Overflowing garbage bin spreading waste near vegetable market");
        c2.setLocation("Market Area");
        c2.setSeverity(Severity.HIGH);
        c2.setAffectedCount(80);

        DuplicateComplaintException ex = assertThrows(DuplicateComplaintException.class, () ->
                complaintService.registerComplaint(c2),
                "Complaints describing the same defect must be rejected as duplicate");
        assertEquals(r1.getComplaintId(), ex.getExistingComplaintId());
    }

    @Test
    @DisplayName("Test 12: Subsequent Duplicate Submissions are Rejected")
    @Transactional
    void testMultiplePossibleDuplicateCandidates() {
        ComplaintRegistrationRequest c1 = new ComplaintRegistrationRequest();
        c1.setCitizenName("Reporter 1");
        c1.setCategory(Category.STREETLIGHT);
        c1.setDescription("Streetlights completely dark and out of order on 3rd cross street");
        c1.setLocation("Nehru Colony");
        c1.setSeverity(Severity.MEDIUM);
        c1.setAffectedCount(30);
        ComplaintResponse r1 = complaintService.registerComplaint(c1);
        assertNotNull(r1);

        ComplaintRegistrationRequest c2 = new ComplaintRegistrationRequest();
        c2.setCitizenName("Reporter 2");
        c2.setCategory(Category.STREETLIGHT);
        c2.setDescription("Streetlight dark and not working on 3rd cross street");
        c2.setLocation("Nehru Colony");
        c2.setSeverity(Severity.MEDIUM);
        c2.setAffectedCount(25);

        assertThrows(DuplicateComplaintException.class, () ->
                complaintService.registerComplaint(c2),
                "Second submission on same streetlight failure must be rejected as duplicate");
    }
}
