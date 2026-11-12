package com.delivery.strategy;

/**
 * Abstract base for delivery fee strategies.
 *
 * <p><b>Inheritance demo:</b> every concrete strategy must return a money amount,
 * and money amounts in this system are always rounded to 2 decimals. Putting the
 * rounding here (Template Method style: {@link #calculateCost(double)} is final)
 * guarantees the invariant once for all current <i>and future</i> strategies —
 * a new pricing rule cannot accidentally produce 3-decimal prices.</p>
 */
public abstract class AbstractDeliveryCostStrategy implements DeliveryCostStrategy {

    /** Final: subclasses implement the raw formula, rounding is non-negotiable. */
    @Override
    public final double calculateCost(double goodsTotal) {
        if (goodsTotal < 0) {
            throw new IllegalArgumentException("Goods total cannot be negative: " + goodsTotal);
        }
        return round2(rawCost(goodsTotal));
    }

    /** The actual pricing formula; must never return a negative value. */
    protected abstract double rawCost(double goodsTotal);

    /** Rounds to kopecks (2 decimals) using HALF_UP — the standard for money. */
    private static double round2(double value) {
        if (value < 0) {
            throw new IllegalStateException("Delivery fee cannot be negative: " + value);
        }
        return Math.round(value * 100.0) / 100.0;
    }
}
