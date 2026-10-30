package com.makkalnagar.service;

import com.makkalnagar.domain.Department;
import com.makkalnagar.domain.Grievance;
import com.makkalnagar.dto.*;
import com.makkalnagar.entity.*;
import com.makkalnagar.enums.*;
import com.makkalnagar.exception.CapacityExceededException;
import com.makkalnagar.exception.DuplicateComplaintException;
import com.makkalnagar.exception.InvalidOperationException;
import com.makkalnagar.exception.ResourceNotFoundException;
import com.makkalnagar.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    private static final Set<String> VALID_LOCATIONS = Set.of(
            "Anna Nagar", "Gandhi Street", "Nehru Colony", "Lake View Road", "Market Area"
    );

    private final ComplaintRepository complaintRepository;
    private final DepartmentRepository departmentRepository;
    private final CitizenRepository citizenRepository;
    private final AssignmentRepository assignmentRepository;
    private final StatusUpdateRepository statusUpdateRepository;
    private final ArrivalSequenceRepository arrivalSequenceRepository;
    private final DepartmentQueueService departmentQueueService;
    private final DuplicateService duplicateService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            DepartmentRepository departmentRepository,
                            CitizenRepository citizenRepository,
                            AssignmentRepository assignmentRepository,
                            StatusUpdateRepository statusUpdateRepository,
                            ArrivalSequenceRepository arrivalSequenceRepository,
                            DepartmentQueueService departmentQueueService,
                            DuplicateService duplicateService) {
        this.complaintRepository = complaintRepository;
        this.departmentRepository = departmentRepository;
        this.citizenRepository = citizenRepository;
        this.assignmentRepository = assignmentRepository;
        this.statusUpdateRepository = statusUpdateRepository;
        this.arrivalSequenceRepository = arrivalSequenceRepository;
        this.departmentQueueService = departmentQueueService;
        this.duplicateService = duplicateService;
    }

    /**
     * Registers a new complaint from a citizen.
     * Enforces unique complaint ID validation, category-to-department routing,
     * automatic duplicate detection, and department capacity verification.
     */
    @Transactional
    public ComplaintResponse registerComplaint(ComplaintRegistrationRequest request) {
        log.info("Processing complaint registration for category={}, location={}",
                request.getCategory(), request.getLocation());

        // 1. Validate location
        String requestedLocation = request.getLocation() != null ? request.getLocation().trim() : "";
        if (!VALID_LOCATIONS.contains(requestedLocation)) {
            throw new InvalidOperationException("Invalid location '" + request.getLocation() +
                    "'. Allowed municipal zones are: " + String.join(", ", VALID_LOCATIONS));
        }

        // 2. Pre-check for duplicate complaints:
        // If an active grievance for this defect already exists in the system (status != RESOLVED),
        // reject and automatically discard it. It is NOT assigned, registered, or saved.
        ComplaintEntity candidate = new ComplaintEntity();
        candidate.setCategory(request.getCategory());
        candidate.setLocation(requestedLocation);
        candidate.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        candidate.setOpenIssueRef(request.getOpenIssueRef() != null && !request.getOpenIssueRef().isBlank() ?
                request.getOpenIssueRef().trim() : null);

        List<ComplaintEntity> activeComplaints = complaintRepository.findByStatusIn(
                List.of(ComplaintStatus.REGISTERED, ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));

        Optional<DuplicateService.DuplicateMatch> duplicateMatch = duplicateService.findDuplicateMatch(candidate, activeComplaints);
        if (duplicateMatch.isPresent()) {
            DuplicateService.DuplicateMatch match = duplicateMatch.get();
            ComplaintEntity existing = match.existingComplaint();
            DuplicateDetectionEngine.DetectionResult result = match.result();
            log.warn("Rejected duplicate complaint submission. Matches existing active complaint {} (confidence: {}%): {}",
                    existing.getComplaintId(), Math.round(result.getConfidenceScore() * 100), result.getReason());

            throw new DuplicateComplaintException(
                    String.format("Duplicate complaint rejected: An active grievance for this issue is already registered under ID %s (%s, Location: %s). " +
                                  "To prevent duplicate department dispatch, this submission was automatically discarded and not assigned or registered.",
                            existing.getComplaintId(), existing.getStatus().getDisplayName(), existing.getLocation()),
                    existing.getComplaintId(),
                    result.getConfidenceScore()
            );
        }

        // 3. Generate deterministic arrival order using database-backed atomic sequence
        ArrivalSequenceEntity seq = arrivalSequenceRepository.findByNameForUpdate("complaint_arrival")
                .orElseGet(() -> arrivalSequenceRepository.save(new ArrivalSequenceEntity("complaint_arrival", 0L)));
        long arrivalOrder = seq.getCurrentVal() + 1;
        seq.setCurrentVal(arrivalOrder);
        arrivalSequenceRepository.save(seq);

        // 3. Unique Complaint ID Validation & Generation
        String complaintId;
        if (request.getComplaintId() != null && !request.getComplaintId().isBlank()) {
            String customId = request.getComplaintId().trim();
            if (complaintRepository.findByComplaintId(customId).isPresent()) {
                throw new InvalidOperationException("Complaint ID conflict: A complaint with ID '" + customId +
                        "' already exists in the system. Repeated complaint IDs are rejected.");
            }
            complaintId = customId;
        } else {
            complaintId = String.format("MN-2026-%04d", arrivalOrder);
            while (complaintRepository.findByComplaintId(complaintId).isPresent()) {
                arrivalOrder++;
                seq.setCurrentVal(arrivalOrder);
                arrivalSequenceRepository.save(seq);
                complaintId = String.format("MN-2026-%04d", arrivalOrder);
            }
        }

        // 4. Resolve or create citizen record
        CitizenEntity citizen = resolveCitizen(request, arrivalOrder);

        // 5. Identify responsible municipal department
        DepartmentEntity department = departmentRepository.findByCode(request.getCategory())
                .orElseThrow(() -> new ResourceNotFoundException("Department for category " + request.getCategory() + " not found"));

        // 6. Build complaint entity (Initial status is always REGISTERED)
        ComplaintEntity complaint = new ComplaintEntity();
        complaint.setComplaintId(complaintId);
        complaint.setCitizen(citizen);
        complaint.setCategory(request.getCategory());
        complaint.setDescription(request.getDescription().trim());
        complaint.setLocation(requestedLocation);
        complaint.setSeverity(request.getSeverity());
        complaint.setAffectedCount(request.getAffectedCount());
        complaint.setArrivalOrder(arrivalOrder);
        complaint.setOpenIssueRef(request.getOpenIssueRef() != null && !request.getOpenIssueRef().isBlank() ?
                request.getOpenIssueRef().trim() : null);
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());
        complaint.setDepartment(department);

        // 7. Check Department Capacity
        long activeCount = complaintRepository.countByDepartmentIdAndStatusIn(
                department.getId(), List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));

        boolean assignedImmediately = activeCount < department.getCapacity();

        if (assignedImmediately) {
            complaint.setStatus(ComplaintStatus.ASSIGNED);
        } else {
            complaint.setStatus(ComplaintStatus.REGISTERED);
        }

        // Save Complaint to MySQL
        complaint = complaintRepository.save(complaint);

        // Audit Trail: Initial Registration Status Update (REGISTERED)
        StatusUpdateEntity regUpdate = new StatusUpdateEntity(
                complaint, null, ComplaintStatus.REGISTERED, "CITIZEN",
                "CITIZEN_REGISTRATION", "Complaint submitted through Makkal Nagar Citizen Portal"
        );
        statusUpdateRepository.save(regUpdate);

        if (assignedImmediately) {
            // Audit Trail: Status Update to ASSIGNED
            StatusUpdateEntity assignUpdate = new StatusUpdateEntity(
                    complaint, ComplaintStatus.REGISTERED, ComplaintStatus.ASSIGNED, "SYSTEM",
                    "AUTOMATIC_CAPACITY_ASSIGNMENT",
                    "Capacity available in " + department.getName() + " (" + activeCount + "/" + department.getCapacity() + "). Assigned immediately."
            );
            statusUpdateRepository.save(assignUpdate);

            // Audit Trail: Assignment record
            AssignmentEntity assignment = new AssignmentEntity(
                    complaint, department, "SYSTEM", "Initial automated routing based on category " + request.getCategory()
            );
            assignmentRepository.save(assignment);
        } else {
            // Department is full: Enqueue into PriorityQueue while remaining in REGISTERED status
            StatusUpdateEntity queueUpdate = new StatusUpdateEntity(
                    complaint, ComplaintStatus.REGISTERED, ComplaintStatus.REGISTERED, "SYSTEM",
                    "DEPARTMENT_QUEUE_WAITING",
                    "Department at maximum capacity (" + activeCount + "/" + department.getCapacity() + "). Placed in priority waiting queue."
            );
            statusUpdateRepository.save(queueUpdate);
            departmentQueueService.enqueueWaiting(complaint);
        }

        // 8. Automatic Duplicate Detection
        // Compare new complaint against all existing system complaints
        List<ComplaintEntity> existingComplaints = complaintRepository.findAll();
        duplicateService.evaluateAndPersistDuplicates(complaint, existingComplaints);

        return mapToResponse(complaint, false);
    }

    private CitizenEntity resolveCitizen(ComplaintRegistrationRequest request, long arrivalOrder) {
        String citizenRef = request.getCitizenReference();
        if (citizenRef != null && !citizenRef.isBlank()) {
            return citizenRepository.findByCitizenReference(citizenRef.trim())
                    .orElseGet(() -> {
                        CitizenEntity newCit = new CitizenEntity(
                                citizenRef.trim(), request.getCitizenName().trim(), request.getPhone(), request.getLocation()
                        );
                        return citizenRepository.save(newCit);
                    });
        }

        // Auto-generate citizen reference if not provided
        String generatedRef = String.format("CIT-%04d", 1000 + arrivalOrder);
        CitizenEntity citizen = new CitizenEntity(
                generatedRef, request.getCitizenName().trim(), request.getPhone(), request.getLocation()
        );
        return citizenRepository.save(citizen);
    }

    /**
     * Operational Event: Assigned department begins processing the complaint.
     * Transitions status automatically: ASSIGNED -> IN_PROGRESS.
     */
    @Transactional
    public ComplaintResponse startDepartmentWork(Long id, String remarks, String actor) {
        ComplaintEntity complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));

        if (complaint.getStatus() != ComplaintStatus.ASSIGNED) {
            throw new InvalidOperationException("Cannot start work on complaint " + complaint.getComplaintId() +
                    ": Current status is " + complaint.getStatus() + ". Work can only start on ASSIGNED complaints.");
        }

        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setUpdatedAt(LocalDateTime.now());
        complaint = complaintRepository.save(complaint);

        String note = (remarks != null && !remarks.isBlank()) ? remarks.trim() : "Department field crew commenced operations on site";
        StatusUpdateEntity statusUpdate = new StatusUpdateEntity(
                complaint, ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS,
                actor != null ? actor : "SYSTEM", "DEPARTMENT_WORK_STARTED", note
        );
        statusUpdateRepository.save(statusUpdate);

        log.info("Complaint {} transitioned to IN_PROGRESS by actor {}: {}", complaint.getComplaintId(), actor, note);
        return mapToResponse(complaint, true);
    }

    /**
     * Resolution Workflow: Verifies resolution details and transitions status: IN_PROGRESS -> RESOLVED.
     * Automatically frees department capacity and assigns the highest-priority eligible waiting complaint.
     */
    @Transactional
    public ComplaintResponse recordVerifiedResolution(Long id, String resolutionNotes, String remarks, String actor) {
        ComplaintEntity complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));

        if (complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {
            throw new InvalidOperationException("Cannot resolve complaint " + complaint.getComplaintId() +
                    ": Current status is " + complaint.getStatus() + ". A complaint must be IN_PROGRESS before resolution can be verified.");
        }

        if (resolutionNotes == null || resolutionNotes.trim().isBlank()) {
            throw new InvalidOperationException("Resolution notes are required to verify and resolve the complaint.");
        }

        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolutionNotes(resolutionNotes.trim());
        complaint.setResolvedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());
        complaint = complaintRepository.save(complaint);

        String note = (remarks != null && !remarks.isBlank()) ? remarks.trim() : ("Resolution verified: " + resolutionNotes.trim());
        StatusUpdateEntity statusUpdate = new StatusUpdateEntity(
                complaint, ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED,
                actor != null ? actor : "SYSTEM", "RESOLUTION_VERIFIED", note
        );
        statusUpdateRepository.save(statusUpdate);

        log.info("Complaint {} resolved and verified by {}. Notes: {}", complaint.getComplaintId(), actor, resolutionNotes);

        // When a complaint is resolved, capacity is freed in that department: auto-dispatch waiting queue!
        if (complaint.getDepartment() != null) {
            departmentQueueService.removeActiveCase(complaint.getDepartment().getCode(), complaint.getComplaintId());
            dispatchNextWaitingComplaint(complaint.getDepartment());
        }

        return mapToResponse(complaint, true);
    }

    /**
     * Rejects arbitrary manual status changes.
     */
    @Transactional
    public ComplaintResponse updateStatus(Long id, StatusUpdateRequest request, String actor) {
        throw new InvalidOperationException("Manual status modification is prohibited. Complaint statuses are managed automatically by system events: 'start-work' triggers IN_PROGRESS and 'resolve' triggers RESOLVED upon verification.");
    }

    /**
     * Reassigns a complaint to another department.
     */
    @Transactional
    public ComplaintResponse reassignDepartment(Long complaintId, AssignRequest request, String actor) {
        ComplaintEntity complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        DepartmentEntity targetDept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Target department not found with ID: " + request.getDepartmentId()));

        DepartmentEntity oldDept = complaint.getDepartment();

        // Check target department capacity if this complaint is currently active
        boolean isActive = complaint.getStatus() == ComplaintStatus.ASSIGNED || complaint.getStatus() == ComplaintStatus.IN_PROGRESS;
        if (isActive) {
            long targetActive = complaintRepository.countByDepartmentIdAndStatusIn(
                    targetDept.getId(), List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));
            if (targetActive >= targetDept.getCapacity()) {
                throw new CapacityExceededException("Target department " + targetDept.getName() +
                        " is at full capacity (" + targetActive + "/" + targetDept.getCapacity() + "). Reassignment rejected.");
            }
        }

        complaint.setDepartment(targetDept);
        complaint.setUpdatedAt(LocalDateTime.now());
        complaintRepository.save(complaint);

        String note = request.getNotes() != null && !request.getNotes().isBlank() ?
                request.getNotes() : "Administrative reassignment from " + (oldDept != null ? oldDept.getName() : "None") + " to " + targetDept.getName();

        AssignmentEntity assignment = new AssignmentEntity(complaint, targetDept, actor, note);
        assignmentRepository.save(assignment);

        StatusUpdateEntity statusUpdate = new StatusUpdateEntity(
                complaint, complaint.getStatus(), complaint.getStatus(), actor,
                "ADMINISTRATIVE_REASSIGNMENT", "Reassigned to " + targetDept.getName() + " (" + note + ")"
        );
        statusUpdateRepository.save(statusUpdate);

        // If old department had an active slot freed, dispatch its waiting queue!
        if (isActive && oldDept != null && !oldDept.getId().equals(targetDept.getId())) {
            departmentQueueService.removeActiveCase(oldDept.getCode(), complaint.getComplaintId());
            dispatchNextWaitingComplaint(oldDept);
        }

        return mapToResponse(complaint, true);
    }

    /**
     * Checks if the department has available capacity and dispatches the highest-priority waiting complaints.
     * Continues processing eligible waiting complaints while capacity remains available.
     * Synchronized to prevent concurrent requests from exceeding department capacity.
     */
    @Transactional
    public synchronized void dispatchNextWaitingComplaint(DepartmentEntity department) {
        if (department == null) return;

        int maxIterations = 50;
        int iterations = 0;

        while (iterations++ < maxIterations) {
            long activeCount = complaintRepository.countByDepartmentIdAndStatusIn(
                    department.getId(), List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));

            if (activeCount >= department.getCapacity()) {
                log.info("Department {} reached capacity ({}/{}). Stopping auto-dispatch.",
                        department.getName(), activeCount, department.getCapacity());
                break;
            }

            List<Grievance> waitingQueue = departmentQueueService.getWaitingQueueSnapshot(department.getCode());
            if (waitingQueue.isEmpty()) {
                log.debug("No waiting complaints in queue for department {}.", department.getName());
                break;
            }

            Grievance topGrievance = waitingQueue.get(0);
            Optional<ComplaintEntity> optComplaint = complaintRepository.findByComplaintId(topGrievance.getComplaintId());

            if (optComplaint.isPresent()) {
                ComplaintEntity waitingComplaint = optComplaint.get();
                if (waitingComplaint.getStatus() == ComplaintStatus.REGISTERED) {
                    waitingComplaint.setStatus(ComplaintStatus.ASSIGNED);
                    waitingComplaint.setUpdatedAt(LocalDateTime.now());
                    complaintRepository.save(waitingComplaint);

                    AssignmentEntity assignment = new AssignmentEntity(
                            waitingComplaint, department, "SYSTEM",
                            "Auto-dispatched from priority waiting queue upon capacity release"
                    );
                    assignmentRepository.save(assignment);

                    StatusUpdateEntity statusUpdate = new StatusUpdateEntity(
                            waitingComplaint, ComplaintStatus.REGISTERED, ComplaintStatus.ASSIGNED, "SYSTEM",
                            "QUEUE_AUTO_DISPATCH",
                            "Promoted from waiting queue to ASSIGNED (Department capacity slot available: " + (activeCount + 1) + "/" + department.getCapacity() + ")"
                    );
                    statusUpdateRepository.save(statusUpdate);

                    departmentQueueService.removeWaiting(waitingComplaint);
                    Department deptDomain = departmentQueueService.getDepartment(department.getCode());
                    if (deptDomain != null) {
                        deptDomain.registerActiveCase(departmentQueueService.toDomain(waitingComplaint));
                    }
                    log.info("Auto-dispatched waiting complaint {} to {} upon capacity release.",
                            waitingComplaint.getComplaintId(), department.getName());
                } else {
                    departmentQueueService.removeWaiting(waitingComplaint);
                }
            } else {
                departmentQueueService.removeWaitingById(department.getCode(), topGrievance.getComplaintId());
            }
        }
    }

    /**
     * Public Tracking API: Returns sanitized complaint status timeline without private citizen details.
     */
    @Transactional(readOnly = true)
    public PublicTrackResponse getPublicTracking(String complaintId) {
        ComplaintEntity complaint = complaintRepository.findByComplaintId(complaintId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Complaint ID '" + complaintId + "' was not found. Please check and try again."));

        PublicTrackResponse response = new PublicTrackResponse();
        response.setComplaintId(complaint.getComplaintId());
        response.setCategory(complaint.getCategory().getDisplayName());
        response.setLocation(complaint.getLocation());
        response.setStatus(complaint.getStatus().name());
        response.setStatusDisplayName(complaint.getStatus().getDisplayName());
        response.setAssignedDepartment(complaint.getDepartment() != null ? complaint.getDepartment().getName() : "Pending Assignment");
        response.setRegistrationDate(complaint.getCreatedAt());
        response.setResolutionNotes(complaint.getResolutionNotes());
        response.setResolutionDate(complaint.getResolvedAt());

        List<StatusUpdateEntity> history = statusUpdateRepository.findByComplaintIdOrderByCreatedAtAsc(complaint.getId());
        List<PublicTrackResponse.PublicStatusEvent> timeline = history.stream()
                .map(u -> new PublicTrackResponse.PublicStatusEvent(
                        u.getNewStatus().name(),
                        u.getNewStatus().getDisplayName(),
                        u.getCreatedAt(),
                        u.getRemarks()
                ))
                .collect(Collectors.toList());
        response.setStatusTimeline(timeline);

        return response;
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Long id) {
        ComplaintEntity complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));
        return mapToResponse(complaint, true);
    }

    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getFilteredComplaints(
            Category category, String location, Severity severity, ComplaintStatus status,
            Long departmentId, String search, Pageable pageable) {

        Page<ComplaintEntity> page = complaintRepository.findFiltered(
                category, location, severity, status, departmentId, search, pageable);

        return page.map(c -> mapToResponse(c, false));
    }

    public ComplaintResponse mapToResponse(ComplaintEntity entity, boolean includeHistory) {
        ComplaintResponse dto = new ComplaintResponse();
        dto.setId(entity.getId());
        dto.setComplaintId(entity.getComplaintId());
        dto.setCitizenName(entity.getCitizen().getName());
        dto.setCitizenReference(entity.getCitizen().getCitizenReference());
        dto.setPhone(entity.getCitizen().getPhone());
        dto.setCategory(entity.getCategory());
        dto.setCategoryDisplayName(entity.getCategory().getDisplayName());
        dto.setDescription(entity.getDescription());
        dto.setLocation(entity.getLocation());
        dto.setSeverity(entity.getSeverity());
        dto.setSeverityDisplayName(entity.getSeverity().getDescription());
        dto.setAffectedCount(entity.getAffectedCount());
        dto.setArrivalOrder(entity.getArrivalOrder());
        dto.setStatus(entity.getStatus());
        dto.setStatusDisplayName(entity.getStatus().getDisplayName());

        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartmentName(entity.getDepartment().getName());
        }

        dto.setOpenIssueRef(entity.getOpenIssueRef());
        dto.setResolutionNotes(entity.getResolutionNotes());
        dto.setResolvedAt(entity.getResolvedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Priority Rank Explanation
        dto.setPriorityRankExplanation(String.format("Severity: %s (Tier %d) -> Impact: %d persons -> Arrival Order: #%d",
                entity.getSeverity().name(), entity.getSeverity().getPriorityWeight(),
                entity.getAffectedCount(), entity.getArrivalOrder()));

        if (includeHistory) {
            List<StatusUpdateEntity> statusList = statusUpdateRepository.findByComplaintIdOrderByCreatedAtAsc(entity.getId());
            dto.setStatusHistory(statusList.stream().map(s -> new ComplaintResponse.StatusUpdateDto(
                    s.getPreviousStatus() != null ? s.getPreviousStatus().name() : null,
                    s.getNewStatus().name(),
                    s.getChangedBy(),
                    s.getTriggeringEvent(),
                    s.getRemarks(),
                    s.getCreatedAt()
            )).collect(Collectors.toList()));

            List<AssignmentEntity> assignList = assignmentRepository.findByComplaintIdOrderByAssignedAtDesc(entity.getId());
            dto.setAssignmentHistory(assignList.stream().map(a -> new ComplaintResponse.AssignmentDto(
                    a.getDepartment().getName(),
                    a.getAssignedBy(),
                    a.getAssignedAt(),
                    a.getNotes()
            )).collect(Collectors.toList()));
        }

        return dto;
    }
}
