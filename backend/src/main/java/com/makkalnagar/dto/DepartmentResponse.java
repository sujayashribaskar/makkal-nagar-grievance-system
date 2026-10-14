package com.makkalnagar.dto;

import com.makkalnagar.enums.Category;

public class DepartmentResponse {

    private Long id;
    private Category code;
    private String name;
    private int capacity;
    private long activeAssignments;
    private long waitingComplaints;
    private long inProgressCount;
    private long resolvedCount;
    private int availableCapacity;
    private double capacityUtilization;

    public DepartmentResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Category getCode() { return code; }
    public void setCode(Category code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public long getActiveAssignments() { return activeAssignments; }
    public void setActiveAssignments(long activeAssignments) { this.activeAssignments = activeAssignments; }

    public long getWaitingComplaints() { return waitingComplaints; }
    public void setWaitingComplaints(long waitingComplaints) { this.waitingComplaints = waitingComplaints; }

    public long getInProgressCount() { return inProgressCount; }
    public void setInProgressCount(long inProgressCount) { this.inProgressCount = inProgressCount; }

    public long getResolvedCount() { return resolvedCount; }
    public void setResolvedCount(long resolvedCount) { this.resolvedCount = resolvedCount; }

    public int getAvailableCapacity() { return availableCapacity; }
    public void setAvailableCapacity(int availableCapacity) { this.availableCapacity = availableCapacity; }

    public double getCapacityUtilization() { return capacityUtilization; }
    public void setCapacityUtilization(double capacityUtilization) { this.capacityUtilization = capacityUtilization; }
}
