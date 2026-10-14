package com.makkalnagar.priority;

import com.makkalnagar.domain.Grievance;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Concrete priority policy implementing the three-tier ranking:
 * 1. Severity: CRITICAL > HIGH > MEDIUM > LOW
 * 2. Impact: Higher affected-person count first
 * 3. Determinism: Earlier arrival order first (FIFO tie-break)
 */
@Component
public class SeverityImpactArrivalPolicy extends GrievancePriorityPolicy {

    @Override
    public Comparator<Grievance> comparator() {
        return Comparator
                .comparingInt((Grievance g) -> g.getSeverity().getPriorityWeight()).reversed()
                .thenComparing(Comparator.comparingInt(Grievance::getAffectedCount).reversed())
                .thenComparingLong(Grievance::getArrivalOrder)
                .thenComparing(Grievance::getComplaintId);
    }
}
