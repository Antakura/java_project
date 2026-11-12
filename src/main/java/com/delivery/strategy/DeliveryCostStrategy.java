package com.delivery.strategy;

/**
 * Strategy pattern: the delivery fee algorithm is a swappable behavior.
 *
 * <p><b>Why this shape?</b> The target schema has no "client district" column, so
 * geographic pricing (the old distance-matrix strategies) is impossible by design.
 * The fee now depends only on the goods total — which is exactly what a real
 * delivery app charges for: a base rate, optionally waived above a threshold.</p>
 *
 * <p>The service layer programs against this interface and never knows which
 * concrete strategy is active (Dependency Inversion): swapping pricing policy is a
 * one-line change in {@code Main}, not a refactor of business logic.</p>
 */
public interface DeliveryCostStrategy {

    /**
     * Calculates the delivery fee for an order.
     *
     * @param goodsTotal sum of all product lines (without the fee itself)
     * @return the fee in rubles, rounded to 2 decimals; never negative
     */
    double calculateCost(double goodsTotal);

    /** Short name shown in logs and UI ("Плоская ставка", "Бесплатно от N ₽"). */
    String describe();
}
