package com.delivery.strategy;

/**
 * Strategy #2: a base fee that becomes free once the goods total reaches a
 * threshold ("бесплатная доставка от 1500 ₽").
 *
 * <p>The classic upsell pricing model: it rewards bigger baskets and is trivially
 * swappable with {@link FlatRateDeliveryStrategy} because both implement the same
 * interface — that interchangeability is the whole point of the Strategy pattern.</p>
 */
public class ThresholdFreeDeliveryStrategy extends AbstractDeliveryCostStrategy {
    private final double baseRate;
    private final double freeAboveTotal;

    public ThresholdFreeDeliveryStrategy(double baseRate, double freeAboveTotal) {
        if (baseRate < 0) {
            throw new IllegalArgumentException("Base rate cannot be negative: " + baseRate);
        }
        if (freeAboveTotal <= 0) {
            throw new IllegalArgumentException(
                    "Free-delivery threshold must be positive: " + freeAboveTotal);
        }
        this.baseRate = baseRate;
        this.freeAboveTotal = freeAboveTotal;
    }

    @Override
    protected double rawCost(double goodsTotal) {
        return goodsTotal >= freeAboveTotal ? 0.0 : baseRate;
    }

    @Override
    public String describe() {
        return "Доставка " + baseRate + " ₽, бесплатно от " + freeAboveTotal + " ₽";
    }
}
