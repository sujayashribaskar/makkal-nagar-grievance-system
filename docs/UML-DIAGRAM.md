# UML Class Diagram & Object-Oriented Design
## Makkal Nagar Municipal Corporation — Grievance Management System

This document visualizes the object-oriented architecture and explains how core OOP concepts are demonstrated in the codebase.

```mermaid
classDiagram
    class Grievance {
        <<abstract>>
        -String complaintId
        -Citizen citizen
        -Category category
        -Severity severity
        -int affectedCount
        -long arrivalOrder
        -String location
        -ComplaintStatus status
        -List~StatusUpdate~ history
        +responsibleDepartment()* String
        +updateStatus(ComplaintStatus, String, String) void
        +getComplaintId() String
        +getCategory() Category
        +getSeverity() Severity
        +getAffectedCount() int
        +getArrivalOrder() long
    }

    class WaterGrievance {
        +responsibleDepartment() String
    }

    class RoadGrievance {
        +responsibleDepartment() String
    }

    class StreetlightGrievance {
        +responsibleDepartment() String
    }

    class WasteGrievance {
        +responsibleDepartment() String
    }

    class GrievancePriorityPolicy {
        <<abstract>>
        +comparator()* Comparator~Grievance~
    }

    class SeverityImpactArrivalPolicy {
        +comparator() Comparator~Grievance~
    }

    class Department {
        -Category category
        -String name
        -int capacity
        -List~Grievance~ assignedCases
        -PriorityQueue~Grievance~ waitingQueue
        +enqueueWaiting(Grievance) void
        +assignNext() Grievance
        +getActiveCount() int
        +isFull() boolean
    }

    class Citizen {
        -String citizenId
        -String name
        -String phone
        -String location
    }

    class Assignment {
        -String complaintId
        -String departmentName
        -String assignedBy
        -LocalDateTime assignedOn
        -String notes
    }

    class StatusUpdate {
        -ComplaintStatus status
        -String remarks
        -String actor
        -LocalDateTime timestamp
    }

    Grievance <|-- WaterGrievance : Inheritance
    Grievance <|-- RoadGrievance : Inheritance
    Grievance <|-- StreetlightGrievance : Inheritance
    Grievance <|-- WasteGrievance : Inheritance

    GrievancePriorityPolicy <|-- SeverityImpactArrivalPolicy : Implementation

    Grievance *-- Citizen : Composition
    Grievance *-- StatusUpdate : Composition
    Department o-- Grievance : Composition (Queue & Assigned)
    Department ..> GrievancePriorityPolicy : Dependency
```

---

### Implementation of Object-Oriented Programming (OOP) Concepts:

1. **Encapsulation**:
   - Class state is kept strictly private (e.g. `Grievance`, `Department`).
   - Mutations are encapsulated behind domain methods (e.g., `Grievance.updateStatus()`, `Department.setCapacity()`).
   - `updateStatus()` validates allowed state machine transitions (`REGISTERED -> ASSIGNED -> IN_PROGRESS -> RESOLVED`) before altering internal state, ensuring integrity.

2. **Inheritance**:
   - `WaterGrievance`, `RoadGrievance`, `StreetlightGrievance`, and `WasteGrievance` inherit all common attributes and methods from the abstract base class `Grievance`.
   - Code duplication is eliminated while allowing category-specific behaviors.

3. **Polymorphism**:
   - The abstract method `responsibleDepartment()` in `Grievance` is polymorphically implemented across subclasses:
     - `WaterGrievance` returns `"Water Supply Department"`.
     - `RoadGrievance` returns `"Roads and Infrastructure Department"`.
     - `StreetlightGrievance` returns `"Streetlight and Electrical Department"`.
     - `WasteGrievance` returns `"Sanitation and Waste Management Department"`.
   - Callers execute `grievance.responsibleDepartment()` without needing `if-else` or `switch` type checks.

4. **Abstraction**:
   - `GrievancePriorityPolicy` provides an abstract contract defining `comparator()`.
   - The concrete class `SeverityImpactArrivalPolicy` encapsulates the multi-tier ranking logic.
   - The `Department` waiting queue relies on the policy abstraction, remaining completely decoupled from how priority scores are computed.

5. **Composition**:
   - A `Department` has-a `PriorityQueue<Grievance>` and has-a `List<Grievance>` of active cases.
   - A `Grievance` has-a `Citizen` and has-a `List<StatusUpdate>` representing its immutable historical audit trail.
