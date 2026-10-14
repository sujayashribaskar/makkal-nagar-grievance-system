-- Flyway Migration: V1__create_schema.sql
-- Makkal Nagar Municipal Corporation Grievance Management System

-- 1. Users Table (for Authentication & Role-based Access)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_ADMIN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Departments Table
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    capacity INT NOT NULL DEFAULT 5,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Citizens Table
CREATE TABLE IF NOT EXISTS citizens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    citizen_reference VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NULL,
    location VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Arrival Sequence Table for safe, atomic arrival order increments
CREATE TABLE IF NOT EXISTS arrival_sequences (
    sequence_name VARCHAR(50) PRIMARY KEY,
    current_val BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Complaints Table
CREATE TABLE IF NOT EXISTS complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id VARCHAR(50) NOT NULL UNIQUE,
    citizen_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    affected_count INT NOT NULL DEFAULT 1,
    arrival_order BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
    department_id BIGINT NULL,
    open_issue_ref VARCHAR(100) NULL,
    resolution_notes TEXT NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_complaint_citizen FOREIGN KEY (citizen_id) REFERENCES citizens (id) ON DELETE RESTRICT,
    CONSTRAINT fk_complaint_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE SET NULL,
    INDEX idx_complaint_status (status),
    INDEX idx_complaint_category (category),
    INDEX idx_complaint_location (location),
    INDEX idx_complaint_severity (severity),
    INDEX idx_complaint_arrival_order (arrival_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Assignments History Table
CREATE TABLE IF NOT EXISTS assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    assigned_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(255) NULL,
    CONSTRAINT fk_assignment_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE CASCADE,
    INDEX idx_assignment_complaint (complaint_id),
    INDEX idx_assignment_dept (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Status Updates History Table
CREATE TABLE IF NOT EXISTS status_updates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    changed_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    remarks TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_status_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
    INDEX idx_status_complaint (complaint_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Duplicate Links Table (Potential duplicate candidates & confirmations)
CREATE TABLE IF NOT EXISTS duplicate_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_complaint_id BIGINT NOT NULL,
    target_complaint_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'FLAGGED',
    reason VARCHAR(255) NOT NULL,
    reviewed_by VARCHAR(100) NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dup_source FOREIGN KEY (source_complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
    CONSTRAINT fk_dup_target FOREIGN KEY (target_complaint_id) REFERENCES complaints (id) ON DELETE CASCADE,
    CONSTRAINT uq_dup_pair UNIQUE (source_complaint_id, target_complaint_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
