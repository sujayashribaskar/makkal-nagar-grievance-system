package com.makkalnagar.repository;

import com.makkalnagar.entity.DuplicateLinkEntity;
import com.makkalnagar.enums.DuplicateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DuplicateLinkRepository extends JpaRepository<DuplicateLinkEntity, Long> {

    @Query("SELECT d FROM DuplicateLinkEntity d WHERE d.sourceComplaint.id = :complaintId OR d.targetComplaint.id = :complaintId")
    List<DuplicateLinkEntity> findAllByComplaintId(@Param("complaintId") Long complaintId);

    @Query("SELECT d FROM DuplicateLinkEntity d WHERE (d.sourceComplaint.id = :c1 AND d.targetComplaint.id = :c2) OR (d.sourceComplaint.id = :c2 AND d.targetComplaint.id = :c1)")
    Optional<DuplicateLinkEntity> findPair(@Param("c1") Long c1, @Param("c2") Long c2);

    List<DuplicateLinkEntity> findByStatus(DuplicateStatus status);
}
