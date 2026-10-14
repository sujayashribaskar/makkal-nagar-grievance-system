package com.makkalnagar.priority;

import com.makkalnagar.domain.Grievance;
import java.util.Comparator;

/**
 * Abstraction:
 * An abstract policy defining the ranking comparator for waiting grievances.
 * Decouples the queuing strategy from the Department queue implementation.
 */
public abstract class GrievancePriorityPolicy {

    /**
     * Supplies the Comparator used by Department PriorityQueue.
     */
    public abstract Comparator<Grievance> comparator();
}
