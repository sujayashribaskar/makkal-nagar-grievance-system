package com.makkalnagar.priority;

import com.makkalnagar.domain.Citizen;
import com.makkalnagar.domain.Grievance;
import com.makkalnagar.domain.WaterGrievance;
import com.makkalnagar.enums.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityQueueAndOrderingTest {

    private GrievancePriorityPolicy policy;
    private PriorityQueue<Grievance> queue;
    private Citizen testCitizen;

    @BeforeEach
    void setUp() {
        policy = new SeverityImpactArrivalPolicy();
        queue = new PriorityQueue<>(policy.comparator());
        testCitizen = new Citizen("C001", "Test Citizen", "9999999999", "Anna Nagar");
    }

    @Test
    @DisplayName("Test 4: Severity ordering - CRITICAL before HIGH before MEDIUM before LOW")
    void testSeverityOrdering() {
        Grievance low = new WaterGrievance("G-LOW", testCitizen, Severity.LOW, 10, 1L, "Anna Nagar", "Low issue", null);
        Grievance med = new WaterGrievance("G-MED", testCitizen, Severity.MEDIUM, 10, 2L, "Anna Nagar", "Medium issue", null);
        Grievance high = new WaterGrievance("G-HIGH", testCitizen, Severity.HIGH, 10, 3L, "Anna Nagar", "High issue", null);
        Grievance crit = new WaterGrievance("G-CRIT", testCitizen, Severity.CRITICAL, 10, 4L, "Anna Nagar", "Critical issue", null);

        // Enqueue in arbitrary order
        queue.add(low);
        queue.add(crit);
        queue.add(med);
        queue.add(high);

        assertEquals("G-CRIT", queue.poll().getComplaintId());
        assertEquals("G-HIGH", queue.poll().getComplaintId());
        assertEquals("G-MED", queue.poll().getComplaintId());
        assertEquals("G-LOW", queue.poll().getComplaintId());
    }

    @Test
    @DisplayName("Test 5: Affected-person tie breaking - Equal severity ranks higher affected count first")
    void testAffectedCountTieBreaking() {
        Grievance g1 = new WaterGrievance("G-20", testCitizen, Severity.HIGH, 20, 1L, "Anna Nagar", "20 affected", null);
        Grievance g2 = new WaterGrievance("G-100", testCitizen, Severity.HIGH, 100, 2L, "Anna Nagar", "100 affected", null);
        Grievance g3 = new WaterGrievance("G-50", testCitizen, Severity.HIGH, 50, 3L, "Anna Nagar", "50 affected", null);

        queue.add(g1);
        queue.add(g2);
        queue.add(g3);

        assertEquals("G-100", queue.poll().getComplaintId(), "Highest affected count (100) must be polled first");
        assertEquals("G-50", queue.poll().getComplaintId(), "Second highest (50) must be polled next");
        assertEquals("G-20", queue.poll().getComplaintId(), "Lowest affected (20) must be polled last");
    }

    @Test
    @DisplayName("Test 6: Arrival-order tie breaking - Equal severity and equal affected count ranks earlier arrival first")
    void testArrivalOrderTieBreaking() {
        Grievance gEarlier = new WaterGrievance("G-EARLY", testCitizen, Severity.HIGH, 50, 10L, "Anna Nagar", "Earlier arrival", null);
        Grievance gLater = new WaterGrievance("G-LATER", testCitizen, Severity.HIGH, 50, 15L, "Anna Nagar", "Later arrival", null);

        queue.add(gLater);
        queue.add(gEarlier);

        assertEquals("G-EARLY", queue.poll().getComplaintId(), "Earlier arrival order (10) must be polled before later (15)");
        assertEquals("G-LATER", queue.poll().getComplaintId());
    }
}
