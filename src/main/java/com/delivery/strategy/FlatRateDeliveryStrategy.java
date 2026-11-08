package com.delivery.strategy;

import com.delivery.model.District;

/**
 * Implementation of DeliveryCostStrategy that uses a fixed rate.
 */
public class FlatRateDeliveryStrategy implements DeliveryCostStrategy {
    private final double flatRate;

    public FlatRateDeliveryStrategy(double flatRate) {
        this.flatRate = flatRate;
    }

    @Override
    public double calculateCost(District from, District to) {
        return flatRate;
    }
}
