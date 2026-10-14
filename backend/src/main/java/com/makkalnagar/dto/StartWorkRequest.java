package com.makkalnagar.dto;

public class StartWorkRequest {
    private String remarks;

    public StartWorkRequest() {}

    public StartWorkRequest(String remarks) {
        this.remarks = remarks;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
