package com.delivery.strategy;

/**
 * Factory Method pattern: creates delivery fee strategies by name (mode).
 *
 * <p><b>Why a factory and not {@code new} in the service?</b> The service depends on
 * the abstraction ({@link DeliveryCostStrategy}) and must not know the concrete
 * classes or their constructor parameters. All "which pricing model do we run today"
 * knowledge is concentrated here, so adding a third strategy means: one new class +
 * one line in this switch — nothing else changes (Open/Closed Principle).</p>
 */
public final class DeliveryCostStrategyFactory {

    /** Pricing models the app can be switched to. */
    public enum Mode {
        FLAT_RATE,
        THRESHOLD_FREE
    }

    private final double flatRate;
    private final double thresholdBaseRate;
    private final double freeAboveTotal;

    public DeliveryCostStrategyFactory(double flatRate,
                                       double thresholdBaseRate,
                                       double freeAboveTotal) {
        this.flatRate = flatRate;
        this.thresholdBaseRate = thresholdBaseRate;
        this.freeAboveTotal = freeAboveTotal;
    }

    /**
     * Creates the strategy for the given pricing mode.
     *
     * @throws IllegalArgumentException on an unknown mode (defensive: a new enum
     *         constant without a case here is a compile-time-visible bug)
     */
    public DeliveryCostStrategy create(Mode mode) {
        return switch (mode) {
            case FLAT_RATE -> new FlatRateDeliveryStrategy(flatRate);
            case THRESHOLD_FREE ->
                    new ThresholdFreeDeliveryStrategy(thresholdBaseRate, freeAboveTotal);
        };
    }
}
