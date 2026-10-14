package com.makkalnagar.controller;

import com.makkalnagar.dto.CapacityUpdateRequest;
import com.makkalnagar.dto.DepartmentResponse;
import com.makkalnagar.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PatchMapping("/{id}/capacity")
    public ResponseEntity<DepartmentResponse> updateCapacity(
            @PathVariable Long id,
            @Valid @RequestBody CapacityUpdateRequest request) {
        DepartmentResponse response = departmentService.updateCapacity(id, request.getCapacity());
        return ResponseEntity.ok(response);
    }
}
