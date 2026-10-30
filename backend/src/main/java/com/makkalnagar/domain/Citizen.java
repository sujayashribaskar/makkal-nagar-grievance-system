package com.makkalnagar.domain;

import java.util.Objects;

/**
 * Encapsulation: encapsulates citizen identity and contact location.
 */
public class Citizen {
    private final String citizenId;
    private final String name;
    private final String phone;
    private final String location;

    public Citizen(String citizenId, String name, String phone, String location) {
        this.citizenId = citizenId;
        this.name = name;
        this.phone = phone;
        this.location = location;
    }

    public String getCitizenId() { return citizenId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getLocation() { return location; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Citizen citizen = (Citizen) o;
        return Objects.equals(citizenId, citizen.citizenId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(citizenId);
    }

    @Override
    public String toString() {
        return name + " (" + citizenId + ", " + location + ")";
    }
}
