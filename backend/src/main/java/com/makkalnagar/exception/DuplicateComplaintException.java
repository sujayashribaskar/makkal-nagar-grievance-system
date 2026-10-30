package com.makkalnagar.exception;

/**
 * Exception thrown when a submitted complaint matches an existing active grievance as a duplicate.
 * Duplicate complaints are rejected immediately without being registered or assigned to any department.
 */
public class DuplicateComplaintException extends RuntimeException {

    private final String existingComplaintId;
    private final double confidenceScore;

    public DuplicateComplaintException(String message) {
        super(message);
        this.existingComplaintId = null;
        this.confidenceScore = 0.0;
    }

    public DuplicateComplaintException(String message, String existingComplaintId, double confidenceScore) {
        super(message);
        this.existingComplaintId = existingComplaintId;
        this.confidenceScore = confidenceScore;
    }

    public String getExistingComplaintId() {
        return existingComplaintId;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }
}
