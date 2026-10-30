package com.makkalnagar.service;

import com.makkalnagar.domain.Department;
import com.makkalnagar.dto.DepartmentResponse;
import com.makkalnagar.entity.DepartmentEntity;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.exception.InvalidOperationException;
import com.makkalnagar.exception.ResourceNotFoundException;
import com.makkalnagar.repository.ComplaintRepository;
import com.makkalnagar.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final ComplaintRepository complaintRepository;
    private final DepartmentQueueService departmentQueueService;

    public DepartmentService(DepartmentRepository departmentRepository,
                             ComplaintRepository complaintRepository,
                             DepartmentQueueService departmentQueueService) {
        this.departmentRepository = departmentRepository;
        this.complaintRepository = complaintRepository;
        this.departmentQueueService = departmentQueueService;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return mapToResponse(entity);
    }

    @Transactional
    public DepartmentResponse updateCapacity(Long id, int newCapacity) {
        if (newCapacity < 1) {
            throw new InvalidOperationException("Department capacity must be at least 1.");
        }

        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        long activeCount = complaintRepository.countByDepartmentIdAndStatusIn(
                entity.getId(), List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));

        if (newCapacity < activeCount) {
            throw new InvalidOperationException(
                    "Cannot reduce department capacity to " + newCapacity +
                    " because there are currently " + activeCount + " active assignments in progress.");
        }

        entity.setCapacity(newCapacity);
        entity = departmentRepository.save(entity);

        // Update in-memory domain Department
        Department domainDept = departmentQueueService.getDepartment(entity.getCode());
        if (domainDept != null) {
            domainDept.setCapacity(newCapacity);
        }

        return mapToResponse(entity);
    }

    public DepartmentResponse mapToResponse(DepartmentEntity entity) {
        long activeCount = complaintRepository.countByDepartmentIdAndStatusIn(
                entity.getId(), List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));
        long waitingCount = complaintRepository.countByDepartmentIdAndStatus(
                entity.getId(), ComplaintStatus.REGISTERED);
        long inProgressCount = complaintRepository.countByDepartmentIdAndStatus(
                entity.getId(), ComplaintStatus.IN_PROGRESS);
        long resolvedCount = complaintRepository.countByDepartmentIdAndStatus(
                entity.getId(), ComplaintStatus.RESOLVED);

        int available = Math.max(0, entity.getCapacity() - (int) activeCount);
        double utilization = entity.getCapacity() > 0 ?
                Math.round(((double) activeCount / entity.getCapacity() * 100.0) * 10.0) / 10.0 : 0.0;

        DepartmentResponse dto = new DepartmentResponse();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setCapacity(entity.getCapacity());
        dto.setActiveAssignments(activeCount);
        dto.setWaitingComplaints(waitingCount);
        dto.setInProgressCount(inProgressCount);
        dto.setResolvedCount(resolvedCount);
        dto.setAvailableCapacity(available);
        dto.setCapacityUtilization(utilization);
        return dto;
    }
}
