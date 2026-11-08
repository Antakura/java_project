package com.delivery.strategy;

import com.delivery.model.District;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistanceBasedDeliveryStrategyTest {

    @Test
    void costGrowsWithDistance() {
        DistanceBasedDeliveryStrategy strategy = new DistanceBasedDeliveryStrategy(2.0);

        // 5 km * 2.0 and 12 km * 2.0 according to the district distance matrix.
        assertEquals(10.0, strategy.calculateCost(District.CENTRAL, District.SUBURB), 0.001);
        assertEquals(24.0, strategy.calculateCost(District.CENTRAL, District.OUTSKIRTS), 0.001);
    }

    @Test
    void distanceIsSymmetric() {
        DistanceBasedDeliveryStrategy strategy = new DistanceBasedDeliveryStrategy(2.0);

        assertEquals(strategy.calculateCost(District.SUBURB, District.CENTRAL),
                strategy.calculateCost(District.CENTRAL, District.SUBURB), 0.001);
    }

    @Test
    void sameDistrictMeansZeroCost() {
        DistanceBasedDeliveryStrategy strategy = new DistanceBasedDeliveryStrategy(2.0);

        assertEquals(0.0, strategy.calculateCost(District.CENTRAL, District.CENTRAL), 0.001);
    }
}
