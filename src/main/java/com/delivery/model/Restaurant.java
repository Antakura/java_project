package com.delivery.model;

/**
 * Represents a restaurant in the system.
 */
public class Restaurant {
    private final String id;
    private final String name;
    private final District district;

    public Restaurant(String id, String name, District district) {
        this.id = id;
        this.name = name;
        this.district = district;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public District getDistrict() {
        return district;
    }

    @Override
    public String toString() {
        return name + " (" + district + ")";
    }
}
