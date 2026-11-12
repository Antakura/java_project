package com.delivery.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThresholdFreeDeliveryStrategyTest {

    @Test
    void costIsBaseRateBelowThreshold() {
        // Base 50, free above 1000
        ThresholdFreeDeliveryStrategy strategy = new ThresholdFreeDeliveryStrategy(50.0, 1000.0);

        assertEquals(50.0, strategy.calculateCost(999.99), 0.001);
    }

    @Test
    void costIsZeroAboveThreshold() {
        // Base 50, free above 1000
        ThresholdFreeDeliveryStrategy strategy = new ThresholdFreeDeliveryStrategy(50.0, 1000.0);

        assertEquals(0.0, strategy.calculateCost(1000.0), 0.001);
        assertEquals(0.0, strategy.calculateCost(2000.0), 0.001);
    }

    @Test
    void describeReturnsCorrectText() {
        ThresholdFreeDeliveryStrategy strategy = new ThresholdFreeDeliveryStrategy(50.0, 1000.0);
        assertEquals("Доставка 50.0 ₽, бесплатно от 1000.0 ₽", strategy.describe());
    }
}
