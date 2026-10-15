package com.makkalnagar.service;

import com.makkalnagar.domain.*;
import com.makkalnagar.entity.ComplaintEntity;
import com.makkalnagar.entity.DepartmentEntity;
import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.priority.GrievancePriorityPolicy;
import com.makkalnagar.repository.ComplaintRepository;
import com.makkalnagar.repository.DepartmentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service managing in-memory Department PriorityQueues and Category mappings.
 *
 * Data Structures:
 * - PriorityQueue<Grievance>: Enforces deterministic priority ordering for waiting complaints.
 * - HashMap<Category, Department>: Provides O(1) lookup to route grievances by category.
 *
 * Reliability:
 * - The waiting queues are reconstructed from MySQL database records upon startup and after updates.
 */
@Service
public class DepartmentQueueService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentQueueService.class);

    private final DepartmentRepository departmentRepository;
    private final ComplaintRepository complaintRepository;
    private final GrievancePriorityPolicy priorityPolicy;

    // HashMap for fast O(1) in-memory routing
    private final Map<Category, Department> categoryToDepartmentMap = new HashMap<>();

    public DepartmentQueueService(DepartmentRepository departmentRepository,
                                  ComplaintRepository complaintRepository,
                                  GrievancePriorityPolicy priorityPolicy) {
        this.departmentRepository = departmentRepository;
        this.complaintRepository = complaintRepository;
        this.priorityPolicy = priorityPolicy;
    }

    @PostConstruct
    public synchronized void initializeQueues() {
        log.info("Initializing in-memory Department queues from MySQL database...");
        categoryToDepartmentMap.clear();

        List<DepartmentEntity> deptEntities = departmentRepository.findAll();
        if (deptEntities.isEmpty()) {
            deptEntities = List.of(
                    departmentRepository.save(new DepartmentEntity(Category.WATER, "Water Supply Department", 5)),
                    departmentRepository.save(new DepartmentEntity(Category.ROAD, "Roads and Infrastructure Department", 5)),
                    departmentRepository.save(new DepartmentEntity(Category.STREETLIGHT, "Streetlight and Electrical Department", 5)),
                    departmentRepository.save(new DepartmentEntity(Category.WASTE, "Sanitation and Waste Management Department", 5))
            );
        }
        for (DepartmentEntity de : deptEntities) {
            Department dept = new Department(de.getCode(), de.getName(), de.getCapacity(), priorityPolicy);
            categoryToDepartmentMap.put(de.getCode(), dept);
        }

        // Hydrate from persistent MySQL complaints
        List<ComplaintEntity> allComplaints = complaintRepository.findAll();
        for (ComplaintEntity entity : allComplaints) {
            Department dept = categoryToDepartmentMap.get(entity.getCategory());
            if (dept == null) continue;

            Grievance domainGrievance = toDomain(entity);
            if (entity.getStatus() == ComplaintStatus.REGISTERED) {
                dept.enqueueWaiting(domainGrievance);
            } else if (entity.getStatus() == ComplaintStatus.ASSIGNED || entity.getStatus() == ComplaintStatus.IN_PROGRESS) {
                dept.registerActiveCase(domainGrievance);
            }
        }

        log.info("Department queues initialized successfully. Registered departments: {}", categoryToDepartmentMap.keySet());
    }

    public synchronized Department getDepartment(Category category) {
        return categoryToDepartmentMap.get(category);
    }

    public synchronized Map<Category, Department> getAllDepartments() {
        return Collections.unmodifiableMap(categoryToDepartmentMap);
    }

    public synchronized void enqueueWaiting(ComplaintEntity entity) {
        Department dept = categoryToDepartmentMap.get(entity.getCategory());
        if (dept != null) {
            dept.enqueueWaiting(toDomain(entity));
            log.debug("Enqueued complaint {} into {} waiting queue. Current queue size: {}",
                    entity.getComplaintId(), dept.getName(), dept.getWaitingCount());
        }
    }

    public synchronized void removeWaiting(ComplaintEntity entity) {
        Department dept = categoryToDepartmentMap.get(entity.getCategory());
        if (dept != null) {
            dept.removeWaiting(toDomain(entity));
        }
    }

    public synchronized List<Grievance> getWaitingQueueSnapshot(Category category) {
        Department dept = categoryToDepartmentMap.get(category);
        if (dept == null) return Collections.emptyList();

        // PriorityQueue iteration is not guaranteed in order; polling a copy provides exact sorted order
        PriorityQueue<Grievance> copy = new PriorityQueue<>(priorityPolicy.comparator());
        copy.addAll(dept.getWaitingQueue());

        List<Grievance> sortedList = new ArrayList<>();
        while (!copy.isEmpty()) {
            sortedList.add(copy.poll());
        }
        return sortedList;
    }

    /**
     * Domain factory demonstrating Polymorphism and Inheritance.
     */
    public Grievance toDomain(ComplaintEntity entity) {
        Citizen citizen = new Citizen(
                entity.getCitizen().getCitizenReference(),
                entity.getCitizen().getName(),
                entity.getCitizen().getPhone(),
                entity.getCitizen().getLocation()
        );

        Grievance g;
        switch (entity.getCategory()) {
            case WATER:
                g = new WaterGrievance(entity.getComplaintId(), citizen, entity.getSeverity(),
                        entity.getAffectedCount(), entity.getArrivalOrder(), entity.getLocation(),
                        entity.getDescription(), entity.getOpenIssueRef());
                break;
            case ROAD:
                g = new RoadGrievance(entity.getComplaintId(), citizen, entity.getSeverity(),
                        entity.getAffectedCount(), entity.getArrivalOrder(), entity.getLocation(),
                        entity.getDescription(), entity.getOpenIssueRef());
                break;
            case STREETLIGHT:
                g = new StreetlightGrievance(entity.getComplaintId(), citizen, entity.getSeverity(),
                        entity.getAffectedCount(), entity.getArrivalOrder(), entity.getLocation(),
                        entity.getDescription(), entity.getOpenIssueRef());
                break;
            case WASTE:
            default:
                g = new WasteGrievance(entity.getComplaintId(), citizen, entity.getSeverity(),
                        entity.getAffectedCount(), entity.getArrivalOrder(), entity.getLocation(),
                        entity.getDescription(), entity.getOpenIssueRef());
                break;
        }

        if (entity.getStatus() != ComplaintStatus.REGISTERED) {
            // Apply status transitions without violating validations
            if (entity.getStatus() == ComplaintStatus.ASSIGNED) {
                g.updateStatus(ComplaintStatus.ASSIGNED, "Assigned", "SYSTEM");
            } else if (entity.getStatus() == ComplaintStatus.IN_PROGRESS) {
                g.updateStatus(ComplaintStatus.ASSIGNED, "Assigned", "SYSTEM");
                g.updateStatus(ComplaintStatus.IN_PROGRESS, "In Progress", "SYSTEM");
            } else if (entity.getStatus() == ComplaintStatus.RESOLVED) {
                g.updateStatus(ComplaintStatus.ASSIGNED, "Assigned", "SYSTEM");
                g.updateStatus(ComplaintStatus.IN_PROGRESS, "In Progress", "SYSTEM");
                g.updateStatus(ComplaintStatus.RESOLVED, "Resolved", "SYSTEM");
            }
        }

        if (entity.getDepartment() != null) {
            g.setAssignedDepartment(entity.getDepartment().getName());
        }

        return g;
    }
}
