package com.makkalnagar.repository;

import com.makkalnagar.entity.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<AssignmentEntity, Long> {
    List<AssignmentEntity> findByComplaintIdOrderByAssignedAtDesc(Long complaintId);
}
