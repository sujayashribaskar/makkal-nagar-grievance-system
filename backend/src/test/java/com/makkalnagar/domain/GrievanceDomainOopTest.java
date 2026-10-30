package com.makkalnagar.domain;

import com.makkalnagar.enums.ComplaintStatus;
import com.makkalnagar.enums.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrievanceDomainOopTest {

    private final Citizen citizen = new Citizen("C001", "Ravi", "9840112233", "Anna Nagar");

    @Test
    @DisplayName("Test Polymorphism: responsibleDepartment() routes each subclass correctly")
    void testSubclassPolymorphism() {
        Grievance water = new WaterGrievance("G1", citizen, Severity.HIGH, 30, 1L, "Anna Nagar", "Water leak", null);
        Grievance road = new RoadGrievance("G2", citizen, Severity.CRITICAL, 50, 2L, "Anna Nagar", "Pothole", null);
        Grievance light = new StreetlightGrievance("G3", citizen, Severity.MEDIUM, 15, 3L, "Anna Nagar", "Dark street", null);
        Grievance waste = new WasteGrievance("G4", citizen, Severity.LOW, 5, 4L, "Anna Nagar", "Trash heap", null);

        assertEquals("Water Supply Department", water.responsibleDepartment());
        assertEquals("Roads and Infrastructure Department", road.responsibleDepartment());
        assertEquals("Streetlight and Electrical Department", light.responsibleDepartment());
        assertEquals("Sanitation and Waste Management Department", waste.responsibleDepartment());
    }

    @Test
    @DisplayName("Test Encapsulation: Valid status lifecycle transitions preserve audit history")
    void testValidStatusLifecycle() {
        Grievance g = new WaterGrievance("G-TEST", citizen, Severity.MEDIUM, 20, 1L, "Anna Nagar", "Test desc", null);
        assertEquals(ComplaintStatus.REGISTERED, g.getStatus());
        assertEquals(1, g.getHistory().size());

        // REGISTERED -> ASSIGNED
        g.updateStatus(ComplaintStatus.ASSIGNED, "Assigned to Water Dept", "SYSTEM");
        assertEquals(ComplaintStatus.ASSIGNED, g.getStatus());
        assertEquals(2, g.getHistory().size());

        // ASSIGNED -> IN_PROGRESS
        g.updateStatus(ComplaintStatus.IN_PROGRESS, "Crew on site", "admin");
        assertEquals(ComplaintStatus.IN_PROGRESS, g.getStatus());
        assertEquals(3, g.getHistory().size());

        // IN_PROGRESS -> RESOLVED
        g.updateStatus(ComplaintStatus.RESOLVED, "Pipeline replaced", "admin");
        assertEquals(ComplaintStatus.RESOLVED, g.getStatus());
        assertEquals(4, g.getHistory().size());
    }

    @Test
    @DisplayName("Test 9: Invalid status transitions throw IllegalStateException")
    void testInvalidStatusTransition() {
        Grievance g = new WaterGrievance("G-INVALID", citizen, Severity.LOW, 10, 1L, "Anna Nagar", "Test desc", null);
        assertEquals(ComplaintStatus.REGISTERED, g.getStatus());

        // Illegal: REGISTERED directly to IN_PROGRESS or RESOLVED
        assertThrows(IllegalStateException.class, () -> g.updateStatus(ComplaintStatus.IN_PROGRESS, "Bypass", "admin"));
        assertThrows(IllegalStateException.class, () -> g.updateStatus(ComplaintStatus.RESOLVED, "Bypass", "admin"));

        // Move to ASSIGNED
        g.updateStatus(ComplaintStatus.ASSIGNED, "Assigned", "admin");

        // Illegal: ASSIGNED directly to RESOLVED
        assertThrows(IllegalStateException.class, () -> g.updateStatus(ComplaintStatus.RESOLVED, "Bypass", "admin"));

        // Move to IN_PROGRESS then RESOLVED
        g.updateStatus(ComplaintStatus.IN_PROGRESS, "In progress", "admin");
        g.updateStatus(ComplaintStatus.RESOLVED, "Resolved", "admin");

        // Illegal: RESOLVED is terminal; cannot transition further
        assertThrows(IllegalStateException.class, () -> g.updateStatus(ComplaintStatus.IN_PROGRESS, "Reopen", "admin"));
    }
}
