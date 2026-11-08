package com.delivery.strategy;

import com.delivery.model.District;

/**
 * Strategy interface for calculating delivery cost based on districts.
 */
public interface DeliveryCostStrategy {
    double calculateCost(District from, District to);
}
