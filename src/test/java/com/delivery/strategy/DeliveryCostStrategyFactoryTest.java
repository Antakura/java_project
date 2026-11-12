package com.delivery.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryCostStrategyFactoryTest {

    @Test
    void factoryReturnsFlatRate() {
        DeliveryCostStrategy strategy = DeliveryCostStrategyFactory.create(DeliveryCostStrategyFactory.Mode.FLAT_RATE, 10.0);
        assertTrue(strategy instanceof FlatRateDeliveryStrategy);
        assertEquals("Плоская ставка: 10.0 ₽ за заказ", strategy.describe());
    }

    @Test
    void factoryReturnsThresholdFree() {
        DeliveryCostStrategy strategy = DeliveryCostStrategyFactory.create(DeliveryCostStrategyFactory.Mode.THRESHOLD_FREE, 50.0, 1000.0);
        assertTrue(strategy instanceof ThresholdFreeDeliveryStrategy);
        assertEquals("Доставка 50.0 ₽, бесплатно от 1000.0 ₽", strategy.describe());
    }
}
