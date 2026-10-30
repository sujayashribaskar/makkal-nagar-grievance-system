package com.makkalnagar.repository;

import com.makkalnagar.entity.ComplaintEntity;
import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<ComplaintEntity, Long> {

    Optional<ComplaintEntity> findByComplaintId(String complaintId);

    boolean existsByComplaintId(String complaintId);

    List<ComplaintEntity> findByDepartmentIdAndStatus(Long departmentId, ComplaintStatus status);

    List<ComplaintEntity> findByDepartmentIdAndStatusIn(Long departmentId, Collection<ComplaintStatus> statuses);

    List<ComplaintEntity> findByStatusIn(Collection<ComplaintStatus> statuses);

    long countByStatus(ComplaintStatus status);

    long countByDepartmentIdAndStatus(Long departmentId, ComplaintStatus status);

    long countByDepartmentIdAndStatusIn(Long departmentId, Collection<ComplaintStatus> statuses);

    long countBySeverityAndStatusNot(Severity severity, ComplaintStatus status);

    List<ComplaintEntity> findByCategoryAndLocationAndStatusIn(
            Category category, String location, Collection<ComplaintStatus> statuses);

    List<ComplaintEntity> findByOpenIssueRefAndStatusIn(String openIssueRef, Collection<ComplaintStatus> statuses);

    @Query("SELECT c FROM ComplaintEntity c WHERE " +
           "(:category IS NULL OR c.category = :category) AND " +
           "(:location IS NULL OR :location = '' OR c.location = :location) AND " +
           "(:severity IS NULL OR c.severity = :severity) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:departmentId IS NULL OR c.department.id = :departmentId) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "  LOWER(c.complaintId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(c.citizen.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ComplaintEntity> findFiltered(
            @Param("category") Category category,
            @Param("location") String location,
            @Param("severity") Severity severity,
            @Param("status") ComplaintStatus status,
            @Param("departmentId") Long departmentId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT c.category, COUNT(c) FROM ComplaintEntity c GROUP BY c.category")
    List<Object[]> countGroupByCategory();

    @Query("SELECT c.status, COUNT(c) FROM ComplaintEntity c GROUP BY c.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT c.department.name, COUNT(c) FROM ComplaintEntity c WHERE c.department IS NOT NULL GROUP BY c.department.name")
    List<Object[]> countGroupByDepartment();

    @Query("SELECT c FROM ComplaintEntity c WHERE c.status <> com.makkalnagar.enums.ComplaintStatus.RESOLVED ORDER BY c.createdAt ASC")
    List<ComplaintEntity> findUnresolvedOrderedByAge();

    @Query("SELECT c FROM ComplaintEntity c WHERE c.status = com.makkalnagar.enums.ComplaintStatus.RESOLVED AND c.resolvedAt IS NOT NULL")
    List<ComplaintEntity> findResolvedComplaints();
}
