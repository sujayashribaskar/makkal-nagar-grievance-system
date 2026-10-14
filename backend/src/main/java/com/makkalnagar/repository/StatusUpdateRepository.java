package com.makkalnagar.repository;

import com.makkalnagar.entity.StatusUpdateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusUpdateRepository extends JpaRepository<StatusUpdateEntity, Long> {
    List<StatusUpdateEntity> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}
