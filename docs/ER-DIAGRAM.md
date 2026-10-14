# Entity Relationship (ER) Diagram
## Makkal Nagar Municipal Corporation — Grievance Management System

This document outlines the relational database schema implemented in MySQL, managed through Flyway migrations.

```mermaid
erDiagram
    USERS {
        BIGINT id PK
        VARCHAR username UK
        VARCHAR password_hash
        VARCHAR full_name
        VARCHAR role
        TIMESTAMP created_at
    }

    DEPARTMENTS {
        BIGINT id PK
        VARCHAR code UK
        VARCHAR name
        INT capacity
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    CITIZENS {
        BIGINT id PK
        VARCHAR citizen_reference UK
        VARCHAR name
        VARCHAR phone
        VARCHAR location
        TIMESTAMP created_at
    }

    ARRIVAL_SEQUENCES {
        VARCHAR sequence_name PK
        BIGINT current_val
    }

    COMPLAINTS {
        BIGINT id PK
        VARCHAR complaint_id UK
        BIGINT citizen_id FK
        VARCHAR category
        TEXT description
        VARCHAR location
        VARCHAR severity
        INT affected_count
        BIGINT arrival_order
        VARCHAR status
        BIGINT department_id FK
        VARCHAR open_issue_ref
        TEXT resolution_notes
        TIMESTAMP resolved_at
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    ASSIGNMENTS {
        BIGINT id PK
        BIGINT complaint_id FK
        BIGINT department_id FK
        VARCHAR assigned_by
        TIMESTAMP assigned_at
        VARCHAR notes
    }

    STATUS_UPDATES {
        BIGINT id PK
        BIGINT complaint_id FK
        VARCHAR previous_status
        VARCHAR new_status
        VARCHAR changed_by
        TEXT remarks
        TIMESTAMP created_at
    }

    DUPLICATE_LINKS {
        BIGINT id PK
        BIGINT source_complaint_id FK
        BIGINT target_complaint_id FK
        VARCHAR status
        VARCHAR reason
        VARCHAR reviewed_by
        TIMESTAMP reviewed_at
        TIMESTAMP created_at
    }

    CITIZENS ||--o{ COMPLAINTS : "submits"
    DEPARTMENTS ||--o{ COMPLAINTS : "manages"
    COMPLAINTS ||--o{ ASSIGNMENTS : "tracks"
    DEPARTMENTS ||--o{ ASSIGNMENTS : "assigned to"
    COMPLAINTS ||--o{ STATUS_UPDATES : "maintains history"
    COMPLAINTS ||--o{ DUPLICATE_LINKS : "source candidate"
    COMPLAINTS ||--o{ DUPLICATE_LINKS : "target candidate"
```

---

### Key Relationships & Data Integrity Constraints:
1. **Citizens to Complaints**: One-to-Many (`citizens.id -> complaints.citizen_id`). Protects citizen profiles from accidental deletion when complaints exist (`ON DELETE RESTRICT`).
2. **Departments to Complaints**: One-to-Many (`departments.id -> complaints.department_id`).
3. **Audit Trails**:
   - `status_updates` maintains an immutable append-only log of lifecycle changes (`REGISTERED -> ASSIGNED -> IN_PROGRESS -> RESOLVED`).
   - `assignments` preserves department routing history and reassignment justifications.
4. **Duplicate Linking**: Many-to-Many self-referential association via `duplicate_links` (`source_complaint_id`, `target_complaint_id`).
5. **Deterministic Arrival Sequence**: `arrival_sequences` table updated via pessimistic row locking to guarantee atomic FIFO arrival indexing without race conditions.
