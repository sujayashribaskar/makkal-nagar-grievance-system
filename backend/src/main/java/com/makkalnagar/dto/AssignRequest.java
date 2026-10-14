package com.makkalnagar.dto;

import jakarta.validation.constraints.NotNull;

public class AssignRequest {

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private String notes;

    public AssignRequest() {}

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
