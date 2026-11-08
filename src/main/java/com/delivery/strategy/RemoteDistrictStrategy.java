package com.delivery.strategy;

import com.delivery.model.District;

/**
 * Remote strategy: higher cost if the destination is an outskirts district.
 */
public class RemoteDistrictStrategy implements DeliveryCostStrategy {
    private final double standardRate;
    private final double remoteRate;

    public RemoteDistrictStrategy(double standardRate, double remoteRate) {
        this.standardRate = standardRate;
        this.remoteRate = remoteRate;
    }

    @Override
    public double calculateCost(District from, District to) {
        if (to.isRemote()) {
            return remoteRate;
        }
        return standardRate;
    }
}
