package com.makkalnagar.repository;

import com.makkalnagar.entity.ArrivalSequenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArrivalSequenceRepository extends JpaRepository<ArrivalSequenceEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ArrivalSequenceEntity s WHERE s.sequenceName = :name")
    Optional<ArrivalSequenceEntity> findByNameForUpdate(@Param("name") String name);

    @Modifying
    @Query("UPDATE ArrivalSequenceEntity s SET s.currentVal = s.currentVal + 1 WHERE s.sequenceName = :name")
    int incrementVal(@Param("name") String name);
}
