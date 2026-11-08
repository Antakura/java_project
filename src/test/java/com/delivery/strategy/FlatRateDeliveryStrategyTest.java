package com.delivery.strategy;

import com.delivery.model.District;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlatRateDeliveryStrategyTest {

    @Test
    void costIsAlwaysTheSameFlatRate() {
        FlatRateDeliveryStrategy strategy = new FlatRateDeliveryStrategy(7.5);

        assertEquals(7.5, strategy.calculateCost(District.CENTRAL, District.OUTSKIRTS), 0.001);
        assertEquals(7.5, strategy.calculateCost(District.SUBURB, District.CENTRAL), 0.001);
    }
}
