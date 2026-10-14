package com.makkalnagar.repository;

import com.makkalnagar.entity.CitizenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CitizenRepository extends JpaRepository<CitizenEntity, Long> {
    Optional<CitizenEntity> findByCitizenReference(String citizenReference);
}
