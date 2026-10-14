package com.makkalnagar.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CapacityUpdateRequest {

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    public CapacityUpdateRequest() {}

    public CapacityUpdateRequest(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}
