package com.makkalnagar.service;

import com.makkalnagar.entity.ComplaintEntity;
import com.makkalnagar.entity.DepartmentEntity;
import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.repository.ComplaintRepository;
import com.makkalnagar.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ComplaintRepository complaintRepository;
    private final DepartmentRepository departmentRepository;

    public ReportService(ComplaintRepository complaintRepository,
                         DepartmentRepository departmentRepository) {
        this.complaintRepository = complaintRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDepartmentReport() {
        List<DepartmentEntity> depts = departmentRepository.findAll();
        List<Map<String, Object>> report = new ArrayList<>();

        for (DepartmentEntity d : depts) {
            long active = complaintRepository.countByDepartmentIdAndStatusIn(
                    d.getId(), List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));
            long waiting = complaintRepository.countByDepartmentIdAndStatus(d.getId(), ComplaintStatus.REGISTERED);
            long resolved = complaintRepository.countByDepartmentIdAndStatus(d.getId(), ComplaintStatus.RESOLVED);
            long total = active + waiting + resolved;
            double utilization = d.getCapacity() > 0 ?
                    Math.round(((double) active / d.getCapacity() * 100.0) * 10.0) / 10.0 : 0.0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("departmentId", d.getId());
            row.put("departmentName", d.getName());
            row.put("capacity", d.getCapacity());
            row.put("activeAssignments", active);
            row.put("waitingComplaints", waiting);
            row.put("resolvedComplaints", resolved);
            row.put("totalComplaints", total);
            row.put("utilizationPercentage", utilization);
            report.add(row);
        }

        // Sorting algorithm requirement: sort by workload descending
        report.sort((a, b) -> Long.compare((long) b.get("totalComplaints"), (long) a.get("totalComplaints")));
        return report;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategoryReport() {
        List<Map<String, Object>> report = new ArrayList<>();
        List<ComplaintEntity> all = complaintRepository.findAll();

        for (Category cat : Category.values()) {
            List<ComplaintEntity> forCat = all.stream().filter(c -> c.getCategory() == cat).collect(Collectors.toList());
            long total = forCat.size();
            long resolved = forCat.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();
            long pending = total - resolved;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", cat.name());
            row.put("displayName", cat.getDisplayName());
            row.put("total", total);
            row.put("pending", pending);
            row.put("resolved", resolved);
            row.put("resolutionRate", total > 0 ? Math.round(((double) resolved / total * 100.0) * 10.0) / 10.0 : 0.0);
            report.add(row);
        }
        return report;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStatusReport() {
        List<Map<String, Object>> report = new ArrayList<>();
        long total = complaintRepository.count();

        for (ComplaintStatus status : ComplaintStatus.values()) {
            long count = complaintRepository.countByStatus(status);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("status", status.name());
            row.put("displayName", status.getDisplayName());
            row.put("count", count);
            row.put("percentage", total > 0 ? Math.round(((double) count / total * 100.0) * 10.0) / 10.0 : 0.0);
            report.add(row);
        }
        return report;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getResolutionTimeReport() {
        List<ComplaintEntity> resolved = complaintRepository.findResolvedComplaints();
        Map<Category, List<Long>> hoursByCategory = new EnumMap<>(Category.class);

        for (Category cat : Category.values()) {
            hoursByCategory.put(cat, new ArrayList<>());
        }

        for (ComplaintEntity c : resolved) {
            if (c.getResolvedAt() != null && c.getCreatedAt() != null) {
                long hours = Duration.between(c.getCreatedAt(), c.getResolvedAt()).toHours();
                hoursByCategory.get(c.getCategory()).add(Math.max(1, hours));
            }
        }

        List<Map<String, Object>> report = new ArrayList<>();
        for (Map.Entry<Category, List<Long>> entry : hoursByCategory.entrySet()) {
            List<Long> durations = entry.getValue();
            double avgHours = durations.isEmpty() ? 0.0 :
                    durations.stream().mapToLong(Long::longValue).average().orElse(0.0);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", entry.getKey().name());
            row.put("displayName", entry.getKey().getDisplayName());
            row.put("resolvedCount", durations.size());
            row.put("avgResolutionHours", Math.round(avgHours * 10.0) / 10.0);
            row.put("avgResolutionDays", Math.round((avgHours / 24.0) * 10.0) / 10.0);
            report.add(row);
        }
        return report;
    }

    /**
     * Demonstrates custom sorting algorithm on pending complaints by age in hours (descending).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingAgeReport() {
        LocalDateTime now = LocalDateTime.now();
        List<ComplaintEntity> unresolved = complaintRepository.findUnresolvedOrderedByAge();

        List<Map<String, Object>> report = unresolved.stream().map(c -> {
            long ageHours = Duration.between(c.getCreatedAt(), now).toHours();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("complaintId", c.getComplaintId());
            row.put("category", c.getCategory().getDisplayName());
            row.put("location", c.getLocation());
            row.put("severity", c.getSeverity().name());
            row.put("status", c.getStatus().getDisplayName());
            row.put("department", c.getDepartment() != null ? c.getDepartment().getName() : "Unassigned");
            row.put("ageHours", ageHours);
            row.put("ageDays", Math.round((ageHours / 24.0) * 10.0) / 10.0);
            row.put("createdDate", c.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            return row;
        }).collect(Collectors.toList());

        // Custom Comparator sorting algorithm: oldest pending complaints first
        report.sort((a, b) -> Long.compare((long) b.get("ageHours"), (long) a.get("ageHours")));
        return report;
    }

    @Transactional(readOnly = true)
    public String exportComplaintsCsv() {
        List<ComplaintEntity> list = complaintRepository.findAll();
        // Sort by arrival order ascending
        list.sort(Comparator.comparingLong(ComplaintEntity::getArrivalOrder));

        StringBuilder sb = new StringBuilder();
        sb.append("Complaint ID,Category,Location,Severity,Affected People,Arrival Order,Status,Department,Citizen Name,Created Date,Resolved Date,Resolution Notes\n");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (ComplaintEntity c : list) {
            sb.append(escapeCsv(c.getComplaintId())).append(",");
            sb.append(escapeCsv(c.getCategory().getDisplayName())).append(",");
            sb.append(escapeCsv(c.getLocation())).append(",");
            sb.append(escapeCsv(c.getSeverity().name())).append(",");
            sb.append(c.getAffectedCount()).append(",");
            sb.append(c.getArrivalOrder()).append(",");
            sb.append(escapeCsv(c.getStatus().name())).append(",");
            sb.append(escapeCsv(c.getDepartment() != null ? c.getDepartment().getName() : "None")).append(",");
            sb.append(escapeCsv(c.getCitizen() != null ? c.getCitizen().getName() : "Anonymous")).append(",");
            sb.append(c.getCreatedAt() != null ? c.getCreatedAt().format(dtf) : "").append(",");
            sb.append(c.getResolvedAt() != null ? c.getResolvedAt().format(dtf) : "").append(",");
            sb.append(escapeCsv(c.getResolutionNotes() != null ? c.getResolutionNotes() : "")).append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"").replace("\n", " ") + "\"";
    }
}
