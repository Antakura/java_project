package com.delivery.strategy;

/**
 * Strategy #1: a fixed fee for every order, no matter how big.
 * The simplest real-world pricing model ("доставка 99 ₽").
 */
public class FlatRateDeliveryStrategy extends AbstractDeliveryCostStrategy {
    private final double rate;

    public FlatRateDeliveryStrategy(double rate) {
        if (rate < 0) {
            throw new IllegalArgumentException("Flat rate cannot be negative: " + rate);
        }
        this.rate = rate;
    }

    @Override
    protected double rawCost(double goodsTotal) {
        return rate;
    }

    @Override
    public String describe() {
        return "Плоская ставка: " + rate + " ₽ за заказ";
    }
}
