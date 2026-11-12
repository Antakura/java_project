package com.delivery.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlatRateDeliveryStrategyTest {

    @Test
    void costIsAlwaysTheSameFlatRate() {
        FlatRateDeliveryStrategy strategy = new FlatRateDeliveryStrategy(7.5);

        // The fee is independent of the goods total (unless negative, which throws)
        assertEquals(7.5, strategy.calculateCost(10.0), 0.001);
        assertEquals(7.5, strategy.calculateCost(1000.0), 0.001);
    }

    @Test
    void describeReturnsCorrectText() {
        FlatRateDeliveryStrategy strategy = new FlatRateDeliveryStrategy(7.5);
        assertEquals("Плоская ставка: 7.5 ₽ за заказ", strategy.describe());
    }
}
