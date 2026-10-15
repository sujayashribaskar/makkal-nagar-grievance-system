-- Flyway Migration: V4__automatic_status_and_duplicate_enhancements.sql
-- Add triggering_event to status_updates and confidence_score, similarity_details to duplicate_links

ALTER TABLE status_updates
    ADD COLUMN triggering_event VARCHAR(100) NOT NULL DEFAULT 'SYSTEM_EVENT';

ALTER TABLE duplicate_links
    MODIFY COLUMN reason VARCHAR(500) NOT NULL,
    ADD COLUMN confidence_score DOUBLE NOT NULL DEFAULT 0.0,
    ADD COLUMN similarity_details TEXT NULL;

