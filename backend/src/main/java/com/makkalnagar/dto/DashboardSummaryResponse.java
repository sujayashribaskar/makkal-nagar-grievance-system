package com.makkalnagar.dto;

import java.util.List;

public class DashboardSummaryResponse {

    private long totalComplaints;
    private long registeredCount;
    private long assignedCount;
    private long inProgressCount;
    private long resolvedCount;
    private long waitingForCapacityCount;
    private long highSeverityUnresolvedCount;

    private List<ComplaintResponse> recentComplaints;
    private List<ComplaintResponse> highestPriorityWaiting;

    public DashboardSummaryResponse() {}

    public long getTotalComplaints() { return totalComplaints; }
    public void setTotalComplaints(long totalComplaints) { this.totalComplaints = totalComplaints; }

    public long getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(long registeredCount) { this.registeredCount = registeredCount; }

    public long getAssignedCount() { return assignedCount; }
    public void setAssignedCount(long assignedCount) { this.assignedCount = assignedCount; }

    public long getInProgressCount() { return inProgressCount; }
    public void setInProgressCount(long inProgressCount) { this.inProgressCount = inProgressCount; }

    public long getResolvedCount() { return resolvedCount; }
    public void setResolvedCount(long resolvedCount) { this.resolvedCount = resolvedCount; }

    public long getWaitingForCapacityCount() { return waitingForCapacityCount; }
    public void setWaitingForCapacityCount(long waitingForCapacityCount) { this.waitingForCapacityCount = waitingForCapacityCount; }

    public long getHighSeverityUnresolvedCount() { return highSeverityUnresolvedCount; }
    public void setHighSeverityUnresolvedCount(long highSeverityUnresolvedCount) { this.highSeverityUnresolvedCount = highSeverityUnresolvedCount; }

    public List<ComplaintResponse> getRecentComplaints() { return recentComplaints; }
    public void setRecentComplaints(List<ComplaintResponse> recentComplaints) { this.recentComplaints = recentComplaints; }

    public List<ComplaintResponse> getHighestPriorityWaiting() { return highestPriorityWaiting; }
    public void setHighestPriorityWaiting(List<ComplaintResponse> highestPriorityWaiting) { this.highestPriorityWaiting = highestPriorityWaiting; }
}
