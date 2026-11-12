package com.delivery.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryCostStrategyFactoryTest {

    private final DeliveryCostStrategyFactory factory =
            new DeliveryCostStrategyFactory(10.0, 50.0, 1000.0);

    @Test
    void factoryReturnsFlatRate() {
        DeliveryCostStrategy strategy = factory.create(DeliveryCostStrategyFactory.Mode.FLAT_RATE);
        assertTrue(strategy instanceof FlatRateDeliveryStrategy);
        assertEquals("Плоская ставка: 10.0 ₽ за заказ", strategy.describe());
    }

    @Test
    void factoryReturnsThresholdFree() {
        DeliveryCostStrategy strategy = factory.create(DeliveryCostStrategyFactory.Mode.THRESHOLD_FREE);
        assertTrue(strategy instanceof ThresholdFreeDeliveryStrategy);
        assertEquals("Доставка 50.0 ₽, бесплатно от 1000.0 ₽", strategy.describe());
    }
}