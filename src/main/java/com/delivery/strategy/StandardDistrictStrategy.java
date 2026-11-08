package com.delivery.strategy;

import com.delivery.model.District;

/**
 * Standard strategy: fixed cost for all deliveries.
 */
public class StandardDistrictStrategy implements DeliveryCostStrategy {
    private final double baseRate;

    public StandardDistrictStrategy(double baseRate) {
        this.baseRate = baseRate;
    }

    @Override
    public double calculateCost(District from, District to) {
        return baseRate;
    }
}
