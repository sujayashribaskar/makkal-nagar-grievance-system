package com.makkalnagar.domain;

import com.makkalnagar.enums.ComplaintStatus;
import java.time.LocalDateTime;

/**
 * Audit record of a complaint status transition.
 */
public class StatusUpdate {
    private final ComplaintStatus status;
    private final String remarks;
    private final String actor;
    private final LocalDateTime timestamp;

    public StatusUpdate(ComplaintStatus status, String remarks, String actor) {
        this.status = status;
        this.remarks = remarks;
        this.actor = actor;
        this.timestamp = LocalDateTime.now();
    }

    public ComplaintStatus getStatus() { return status; }
    public String getRemarks() { return remarks; }
    public String getActor() { return actor; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + status + " by " + actor + " - " + remarks;
    }
}
