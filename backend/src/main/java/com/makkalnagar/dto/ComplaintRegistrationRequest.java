package com.makkalnagar.dto;

import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.Severity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ComplaintRegistrationRequest {

    @NotBlank(message = "Citizen name is required")
    @Size(min = 2, max = 100, message = "Citizen name must be between 2 and 100 characters")
    private String citizenName;

    private String citizenReference;

    @Size(max = 20, message = "Contact phone cannot exceed 20 characters")
    private String phone;

    @NotNull(message = "Category is required")
    private Category category;

    @NotBlank(message = "Complaint description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Severity is required")
    private Severity severity;

    @NotNull(message = "Number of affected people is required")
    @Min(value = 1, message = "Affected people count must be at least 1")
    private Integer affectedCount;

    @Size(max = 100, message = "Open issue reference cannot exceed 100 characters")
    private String openIssueRef;

    public ComplaintRegistrationRequest() {}

    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }

    public String getCitizenReference() { return citizenReference; }
    public void setCitizenReference(String citizenReference) { this.citizenReference = citizenReference; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public Integer getAffectedCount() { return affectedCount; }
    public void setAffectedCount(Integer affectedCount) { this.affectedCount = affectedCount; }

    public String getOpenIssueRef() { return openIssueRef; }
    public void setOpenIssueRef(String openIssueRef) { this.openIssueRef = openIssueRef; }
}
