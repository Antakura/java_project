package com.delivery.model;

/**
 * Represents a courier responsible for delivery.
 */
public class Courier extends User {
    private final String vehicleType;

    public Courier(String id, String name, String phoneNumber, String vehicleType) {
        super(id, name, phoneNumber);
        this.vehicleType = vehicleType;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    @Override
    public String toString() {
        return super.toString() + " [vehicle=" + vehicleType + "]";
    }
}
