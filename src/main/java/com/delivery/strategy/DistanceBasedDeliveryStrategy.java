package com.delivery.strategy;

import com.delivery.model.District;

/**
 * Implementation of DeliveryCostStrategy that calculates cost based on the distance between districts.
 */
public class DistanceBasedDeliveryStrategy implements DeliveryCostStrategy {
    private final double costPerKm;

    public DistanceBasedDeliveryStrategy(double costPerKm) {
        this.costPerKm = costPerKm;
    }

    @Override
    public double calculateCost(District from, District to) {
        return from.distanceTo(to) * costPerKm;
    }
}
