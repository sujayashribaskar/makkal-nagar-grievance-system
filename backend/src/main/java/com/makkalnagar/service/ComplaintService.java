package com.makkalnagar.service;

import com.makkalnagar.domain.Grievance;
import com.makkalnagar.dto.*;
import com.makkalnagar.entity.*;
import com.makkalnagar.enums.*;
import com.makkalnagar.exception.CapacityExceededException;
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
    private final DuplicateLinkRepository duplicateLinkRepository;
    private final ArrivalSequenceRepository arrivalSequenceRepository;
    private final DepartmentQueueService departmentQueueService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            DepartmentRepository departmentRepository,
                            CitizenRepository citizenRepository,
                            AssignmentRepository assignmentRepository,
                            StatusUpdateRepository statusUpdateRepository,
                            DuplicateLinkRepository duplicateLinkRepository,
                            ArrivalSequenceRepository arrivalSequenceRepository,
                            DepartmentQueueService departmentQueueService) {
        this.complaintRepository = complaintRepository;
        this.departmentRepository = departmentRepository;
        this.citizenRepository = citizenRepository;
        this.assignmentRepository = assignmentRepository;
        this.statusUpdateRepository = statusUpdateRepository;
        this.duplicateLinkRepository = duplicateLinkRepository;
        this.arrivalSequenceRepository = arrivalSequenceRepository;
        this.departmentQueueService = departmentQueueService;
    }

    /**
     * Registers a new complaint from a citizen.
     * Enforces atomic arrival ordering, category-to-department mapping,
     * duplicate candidate checks, and department capacity verification.
     */
    @Transactional
    public ComplaintResponse registerComplaint(ComplaintRegistrationRequest request) {
        log.info("Processing complaint registration for category={}, location={}",
                request.getCategory(), request.getLocation());

        // 1. Validate location
        if (!VALID_LOCATIONS.contains(request.getLocation())) {
            throw new InvalidOperationException("Invalid location '" + request.getLocation() +
                    "'. Allowed municipal zones are: " + String.join(", ", VALID_LOCATIONS));
        }

        // 2. Safely generate deterministic arrival order using database-backed atomic sequence
        ArrivalSequenceEntity seq = arrivalSequenceRepository.findByNameForUpdate("complaint_arrival")
                .orElseGet(() -> arrivalSequenceRepository.save(new ArrivalSequenceEntity("complaint_arrival", 0L)));
        long arrivalOrder = seq.getCurrentVal() + 1;
        seq.setCurrentVal(arrivalOrder);
        arrivalSequenceRepository.save(seq);

        // 3. Generate unique municipal complaint ID
        String complaintId = String.format("MN-2026-%04d", arrivalOrder);

        // 4. Resolve or create citizen record
        CitizenEntity citizen = resolveCitizen(request, arrivalOrder);

        // 5. Identify responsible municipal department
        DepartmentEntity department = departmentRepository.findByCode(request.getCategory())
                .orElseThrow(() -> new ResourceNotFoundException("Department for category " + request.getCategory() + " not found"));

        // 6. Build complaint entity
        ComplaintEntity complaint = new ComplaintEntity();
        complaint.setComplaintId(complaintId);
        complaint.setCitizen(citizen);
        complaint.setCategory(request.getCategory());
        complaint.setDescription(request.getDescription().trim());
        complaint.setLocation(request.getLocation());
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

        // Audit Trail: Initial Registration Status Update
        StatusUpdateEntity regUpdate = new StatusUpdateEntity(
                complaint, null, ComplaintStatus.REGISTERED, "CITIZEN", "Complaint submitted through Makkal Nagar Citizen Portal"
        );
        statusUpdateRepository.save(regUpdate);

        if (assignedImmediately) {
            // Audit Trail: Status Update to ASSIGNED
            StatusUpdateEntity assignUpdate = new StatusUpdateEntity(
                    complaint, ComplaintStatus.REGISTERED, ComplaintStatus.ASSIGNED, "SYSTEM",
                    "Capacity available in " + department.getName() + " (" + activeCount + "/" + department.getCapacity() + "). Assigned immediately."
            );
            statusUpdateRepository.save(assignUpdate);

            // Audit Trail: Assignment record
            AssignmentEntity assignment = new AssignmentEntity(
                    complaint, department, "SYSTEM", "Initial automated routing based on category " + request.getCategory()
            );
            assignmentRepository.save(assignment);
        } else {
            // Department is full: Enqueue into in-memory PriorityQueue
            StatusUpdateEntity queueUpdate = new StatusUpdateEntity(
                    complaint, ComplaintStatus.REGISTERED, ComplaintStatus.REGISTERED, "SYSTEM",
                    "Department at maximum capacity (" + activeCount + "/" + department.getCapacity() + "). Placed in priority waiting queue."
            );
            statusUpdateRepository.save(queueUpdate);
            departmentQueueService.enqueueWaiting(complaint);
        }

        // 8. Duplicate Detection Check
        checkAndRecordPotentialDuplicates(complaint);

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
     * Checks for open complaints in the same category & location, or matching open issue references.
     * Flags candidates for administrative operator review without auto-deleting or merging.
     */
    private void checkAndRecordPotentialDuplicates(ComplaintEntity newComplaint) {
        List<ComplaintStatus> openStatuses = List.of(ComplaintStatus.REGISTERED, ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS);

        // Find matches in same category & location
        List<ComplaintEntity> candidates = complaintRepository.findByCategoryAndLocationAndStatusIn(
                newComplaint.getCategory(), newComplaint.getLocation(), openStatuses);

        for (ComplaintEntity existing : candidates) {
            if (existing.getId().equals(newComplaint.getId())) continue;

            String reason = "Shares identical Category (" + newComplaint.getCategory().getDisplayName() +
                    ") and Location (" + newComplaint.getLocation() + ") with open complaint " + existing.getComplaintId();

            if (newComplaint.getOpenIssueRef() != null &&
                newComplaint.getOpenIssueRef().equalsIgnoreCase(existing.getOpenIssueRef())) {
                reason += " and matching Open Issue Reference [" + newComplaint.getOpenIssueRef() + "]";
            }

            if (duplicateLinkRepository.findPair(newComplaint.getId(), existing.getId()).isEmpty()) {
                DuplicateLinkEntity link = new DuplicateLinkEntity(newComplaint, existing, DuplicateStatus.FLAGGED, reason);
                duplicateLinkRepository.save(link);
                log.info("Flagged possible duplicate candidate between {} and {}: {}",
                        newComplaint.getComplaintId(), existing.getComplaintId(), reason);
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

    /**
     * Updates complaint status with validation.
     * When a complaint is marked RESOLVED, releases department capacity and
     * automatically pulls and assigns the highest-priority waiting complaint from that department.
     */
    @Transactional
    public ComplaintResponse updateStatus(Long id, StatusUpdateRequest request, String actor) {
        ComplaintEntity complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));

        ComplaintStatus current = complaint.getStatus();
        ComplaintStatus next = request.getNewStatus();

        if (!current.canTransitionTo(next)) {
            throw new InvalidOperationException("Illegal status transition for complaint " +
                    complaint.getComplaintId() + ": Cannot transition from " + current + " to " + next);
        }

        if (next == ComplaintStatus.RESOLVED) {
            if (request.getResolutionNotes() == null || request.getResolutionNotes().trim().isBlank()) {
                throw new InvalidOperationException("Resolution notes are required when marking a complaint RESOLVED.");
            }
            complaint.setResolutionNotes(request.getResolutionNotes().trim());
            complaint.setResolvedAt(LocalDateTime.now());
        }

        complaint.setStatus(next);
        complaint.setUpdatedAt(LocalDateTime.now());
        complaint = complaintRepository.save(complaint);

        String remark = request.getRemarks() != null && !request.getRemarks().isBlank() ?
                request.getRemarks().trim() : ("Status updated to " + next.getDisplayName());
        if (next == ComplaintStatus.RESOLVED) {
            remark = "Resolved: " + complaint.getResolutionNotes();
        }

        StatusUpdateEntity statusUpdate = new StatusUpdateEntity(complaint, current, next, actor, remark);
        statusUpdateRepository.save(statusUpdate);

        // When a complaint is resolved, capacity is freed in that department!
        if (next == ComplaintStatus.RESOLVED && complaint.getDepartment() != null) {
            dispatchNextWaitingComplaint(complaint.getDepartment());
        }

        return mapToResponse(complaint, true);
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
                complaint, complaint.getStatus(), complaint.getStatus(), actor, "Reassigned to " + targetDept.getName() + " (" + note + ")"
        );
        statusUpdateRepository.save(statusUpdate);

        // If old department had an active slot freed, dispatch its waiting queue!
        if (isActive && oldDept != null && !oldDept.getId().equals(targetDept.getId())) {
            dispatchNextWaitingComplaint(oldDept);
        }

        return mapToResponse(complaint, true);
    }

    /**
     * Checks if the department has available capacity and dispatches the highest-priority waiting complaint.
     */
    @Transactional
    public void dispatchNextWaitingComplaint(DepartmentEntity department) {
        long activeCount = complaintRepository.countByDepartmentIdAndStatusIn(
                department.getId(), List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));

        if (activeCount >= department.getCapacity()) {
            return; // Department is full
        }

        // Pull highest priority waiting complaint using domain policy
        List<Grievance> waitingQueue = departmentQueueService.getWaitingQueueSnapshot(department.getCode());
        if (waitingQueue.isEmpty()) {
            return; // No waiting complaints
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
                        "Promoted from waiting queue to ASSIGNED (Department capacity slot available)"
                );
                statusUpdateRepository.save(statusUpdate);

                // Update in-memory queue
                departmentQueueService.removeWaiting(waitingComplaint);
                log.info("Auto-dispatched waiting complaint {} to {} upon capacity release.",
                        waitingComplaint.getComplaintId(), department.getName());
            }
        }
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
