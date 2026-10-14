package com.makkalnagar.service;

import com.makkalnagar.dto.DepartmentResponse;
import com.makkalnagar.entity.DepartmentEntity;
import com.makkalnagar.enums.Category;
import com.makkalnagar.exception.InvalidOperationException;
import com.makkalnagar.repository.DepartmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DepartmentCapacityTest {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    @DisplayName("Test 15: Validates capacity changes; rejects capacity below active assignments")
    @Transactional
    void testCapacityChangeValidation() {
        DepartmentEntity waterDept = departmentRepository.findByCode(Category.WATER).orElseThrow();
        DepartmentResponse deptResponse = departmentService.getDepartmentById(waterDept.getId());

        long activeCount = deptResponse.getActiveAssignments();

        if (activeCount > 0) {
            // Attempting to set capacity to less than active count must throw InvalidOperationException
            int illegalCapacity = (int) activeCount - 1;
            if (illegalCapacity >= 1) {
                assertThrows(InvalidOperationException.class, () ->
                        departmentService.updateCapacity(waterDept.getId(), illegalCapacity));
            }
        }

        // Setting capacity equal to or above active count must succeed
        int validCapacity = Math.max((int) activeCount, 5) + 2;
        DepartmentResponse updated = departmentService.updateCapacity(waterDept.getId(), validCapacity);
        assertEquals(validCapacity, updated.getCapacity());
    }
}
