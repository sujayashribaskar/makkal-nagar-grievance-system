package com.makkalnagar.repository;

import com.makkalnagar.entity.DepartmentEntity;
import com.makkalnagar.enums.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {
    Optional<DepartmentEntity> findByCode(Category code);
    Optional<DepartmentEntity> findByName(String name);
}
