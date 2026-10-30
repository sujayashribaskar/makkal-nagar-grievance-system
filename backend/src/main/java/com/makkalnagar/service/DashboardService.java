package com.makkalnagar.service;

import com.makkalnagar.dto.ChartDataResponse;
import com.makkalnagar.dto.ComplaintResponse;
import com.makkalnagar.dto.DashboardSummaryResponse;
import com.makkalnagar.entity.ComplaintEntity;
import com.makkalnagar.entity.DepartmentEntity;
import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.enums.Severity;
import com.makkalnagar.repository.ComplaintRepository;
import com.makkalnagar.repository.DepartmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ComplaintRepository complaintRepository;
    private final DepartmentRepository departmentRepository;
    private final ComplaintService complaintService;
    private final com.makkalnagar.repository.DuplicateLinkRepository duplicateLinkRepository;

    public DashboardService(ComplaintRepository complaintRepository,
                            DepartmentRepository departmentRepository,
                            ComplaintService complaintService,
                            com.makkalnagar.repository.DuplicateLinkRepository duplicateLinkRepository) {
        this.complaintRepository = complaintRepository;
        this.departmentRepository = departmentRepository;
        this.complaintService = complaintService;
        this.duplicateLinkRepository = duplicateLinkRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long total = complaintRepository.count();
        long registered = complaintRepository.countByStatus(ComplaintStatus.REGISTERED);
        long assigned = complaintRepository.countByStatus(ComplaintStatus.ASSIGNED);
        long inProgress = complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS);
        long resolved = complaintRepository.countByStatus(ComplaintStatus.RESOLVED);
        long highSeverityUnresolved = complaintRepository.countBySeverityAndStatusNot(Severity.CRITICAL, ComplaintStatus.RESOLVED) +
                                      complaintRepository.countBySeverityAndStatusNot(Severity.HIGH, ComplaintStatus.RESOLVED);

        // Recent 5 complaints
        List<ComplaintEntity> recentEntities = complaintRepository.findAll(
                PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
        List<ComplaintResponse> recentDtos = recentEntities.stream()
                .map(c -> complaintService.mapToResponse(c, false))
                .collect(Collectors.toList());

        // Highest priority waiting complaints (Ordered by Severity DESC, AffectedCount DESC, ArrivalOrder ASC)
        List<ComplaintEntity> waitingEntities = complaintRepository.findAll(
                PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "arrivalOrder"))
        ).getContent().stream()
                .filter(c -> c.getStatus() == ComplaintStatus.REGISTERED)
                .sorted(Comparator.comparingInt((ComplaintEntity c) -> c.getSeverity().getPriorityWeight()).reversed()
                        .thenComparing(Comparator.comparingInt((ComplaintEntity c) -> c.getAffectedCount()).reversed())
                        .thenComparingLong((ComplaintEntity c) -> c.getArrivalOrder()))
                .limit(6)
                .collect(Collectors.toList());

        List<ComplaintResponse> waitingDtos = waitingEntities.stream()
                .map(c -> complaintService.mapToResponse(c, false))
                .collect(Collectors.toList());

        DashboardSummaryResponse summary = new DashboardSummaryResponse();
        summary.setTotalComplaints(total);
        summary.setRegisteredCount(registered);
        summary.setAssignedCount(assigned);
        summary.setInProgressCount(inProgress);
        summary.setResolvedCount(resolved);
        summary.setWaitingForCapacityCount(registered);
        summary.setHighSeverityUnresolvedCount(highSeverityUnresolved);
        summary.setDuplicatesFlaggedCount(duplicateLinkRepository.count());
        summary.setRecentComplaints(recentDtos);
        summary.setHighestPriorityWaiting(waitingDtos);

        return summary;
    }

    @Transactional(readOnly = true)
    public ChartDataResponse getCharts() {
        ChartDataResponse chartResponse = new ChartDataResponse();

        // 1. Status Chart
        List<String> statusLabels = List.of("Registered / Waiting", "Assigned", "In Progress", "Resolved");
        List<Long> statusData = List.of(
                complaintRepository.countByStatus(ComplaintStatus.REGISTERED),
                complaintRepository.countByStatus(ComplaintStatus.ASSIGNED),
                complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS),
                complaintRepository.countByStatus(ComplaintStatus.RESOLVED)
        );
        chartResponse.setStatusChart(new ChartDataResponse.SeriesData(statusLabels, statusData));

        // 2. Category Distribution Chart
        List<String> categoryLabels = new ArrayList<>();
        List<Long> categoryData = new ArrayList<>();
        for (Category cat : Category.values()) {
            categoryLabels.add(cat.getDisplayName());
            long count = complaintRepository.findAll().stream()
                    .filter(c -> c.getCategory() == cat)
                    .count();
            categoryData.add(count);
        }
        chartResponse.setCategoryChart(new ChartDataResponse.SeriesData(categoryLabels, categoryData));

        // 3. Department Workload Chart
        List<DepartmentEntity> departments = departmentRepository.findAll();
        List<String> deptLabels = new ArrayList<>();
        List<Integer> capacities = new ArrayList<>();
        List<Long> activeCounts = new ArrayList<>();
        List<Long> waitingCounts = new ArrayList<>();

        for (DepartmentEntity dept : departments) {
            deptLabels.add(dept.getName());
            capacities.add(dept.getCapacity());
            long active = complaintRepository.countByDepartmentIdAndStatusIn(
                    dept.getId(), List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));
            long waiting = complaintRepository.countByDepartmentIdAndStatus(
                    dept.getId(), ComplaintStatus.REGISTERED);
            activeCounts.add(active);
            waitingCounts.add(waiting);
        }

        chartResponse.setDepartmentWorkloadChart(
                new ChartDataResponse.DepartmentWorkloadChart(deptLabels, capacities, activeCounts, waitingCounts)
        );

        return chartResponse;
    }
}
