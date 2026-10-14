-- Flyway Migration: V2__seed_initial_data.sql
-- Departments, sequence initialization, and initial admin user

-- 1. Initialize arrival sequence
INSERT INTO arrival_sequences (sequence_name, current_val)
VALUES ('complaint_arrival', 0)
ON DUPLICATE KEY UPDATE sequence_name = sequence_name;

-- 2. Seed the 4 official municipal departments
INSERT INTO departments (code, name, capacity) VALUES
('WATER', 'Water Supply Department', 5),
('ROAD', 'Roads and Infrastructure Department', 5),
('STREETLIGHT', 'Streetlight and Electrical Department', 5),
('WASTE', 'Sanitation and Waste Management Department', 5)
ON DUPLICATE KEY UPDATE name = VALUES(name), capacity = VALUES(capacity);

-- 3. Initial Administrator User (admin / Admin@Makkal2026)
-- Password hash generated with standard BCrypt strength 10 for: Admin@Makkal2026
INSERT INTO users (username, password_hash, full_name, role) VALUES
('admin', '$2a$10$7R3Qz3z7f1uFz8vB0o6LKeLqM5T1J.X0N5k.B0Q9H1mG4lM6mB5x2', 'Chief Municipal Administrator', 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE username = username;
