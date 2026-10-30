package com.makkalnagar.domain;

import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.priority.GrievancePriorityPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Composition:
 * A Department owns its own waiting queue (PriorityQueue) and assigned cases list (ArrayList).
 *
 * Data Structure: java.util.PriorityQueue<Grievance>
 * - Provides logarithmic O(log n) time complexity for insertions and poll() operations.
 * - Enforces deterministic priority ranking via GrievancePriorityPolicy.
 */
public class Department {

    private final Category category;
    private final String name;
    private int capacity;
    private final List<Grievance> assignedCases;
    private final PriorityQueue<Grievance> waitingQueue;

    public Department(Category category, String name, int capacity, GrievancePriorityPolicy policy) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Department capacity must be at least 1");
        }
        this.category = category;
        this.name = name;
        this.capacity = capacity;
        this.assignedCases = new ArrayList<>();
        this.waitingQueue = new PriorityQueue<>(policy.comparator());
    }

    public Category getCategory() { return category; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }

    public synchronized void setCapacity(int capacity) {
        if (capacity < getActiveCount()) {
            throw new IllegalArgumentException(
                    "New capacity (" + capacity + ") cannot be less than active assignments (" + getActiveCount() + ")");
        }
        this.capacity = capacity;
    }

    public synchronized int getActiveCount() {
        return (int) assignedCases.stream()
                .filter(g -> g.getStatus() == ComplaintStatus.ASSIGNED || g.getStatus() == ComplaintStatus.IN_PROGRESS)
                .count();
    }

    public synchronized boolean isFull() {
        return getActiveCount() >= capacity;
    }

    /**
     * Enqueues or re-enqueues a grievance into the priority waiting queue.
     * Removing and re-adding guarantees re-heapification in case priority attributes changed.
     */
    public synchronized void enqueueWaiting(Grievance g) {
        if (g == null) return;
        waitingQueue.removeIf(item -> item.getComplaintId().equals(g.getComplaintId()));
        waitingQueue.add(g);
    }

    public synchronized boolean removeWaiting(Grievance g) {
        if (g == null) return false;
        return waitingQueue.removeIf(item -> item.getComplaintId().equals(g.getComplaintId()));
    }

    public synchronized boolean removeWaitingById(String complaintId) {
        if (complaintId == null) return false;
        return waitingQueue.removeIf(item -> item.getComplaintId().equals(complaintId));
    }

    public synchronized void removeActiveCase(String complaintId) {
        if (complaintId != null) {
            assignedCases.removeIf(item -> item.getComplaintId().equals(complaintId));
        }
    }

    /**
     * Pulls the highest-priority waiting complaint and assigns it to this department
     * if capacity is available. Returns null if full or if queue is empty.
     */
    public synchronized Grievance assignNext() {
        if (isFull() || waitingQueue.isEmpty()) {
            return null;
        }
        Grievance next = waitingQueue.poll();
        assignedCases.add(next);
        next.setAssignedDepartment(this.name);
        next.updateStatus(ComplaintStatus.ASSIGNED, "Assigned to " + this.name, "SYSTEM");
        return next;
    }

    public synchronized void registerActiveCase(Grievance g) {
        if (g != null && !assignedCases.contains(g)) {
            assignedCases.add(g);
            g.setAssignedDepartment(this.name);
        }
    }

    public synchronized PriorityQueue<Grievance> getWaitingQueue() {
        return waitingQueue;
    }

    public synchronized List<Grievance> getAssignedCases() {
        return Collections.unmodifiableList(assignedCases);
    }

    public synchronized int getWaitingCount() {
        return waitingQueue.size();
    }

    @Override
    public String toString() {
        return String.format("%-25s | Capacity: %d | Active: %d | Waiting: %d",
                name, capacity, getActiveCount(), waitingQueue.size());
    }
}
