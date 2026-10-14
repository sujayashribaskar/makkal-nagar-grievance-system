package com.makkalnagar.dto;

import jakarta.validation.constraints.NotBlank;

public class ResolveComplaintRequest {

    @NotBlank(message = "Resolution notes are required")
    private String resolutionNotes;

    private String remarks;

    public ResolveComplaintRequest() {}

    public ResolveComplaintRequest(String resolutionNotes, String remarks) {
        this.resolutionNotes = resolutionNotes;
        this.remarks = remarks;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
