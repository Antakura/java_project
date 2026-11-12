package com.delivery.model;

import java.util.Set;

/**
 * Possible states of an order.
 *
 * <p><b>State pattern (tabular form).</b> Each state knows which transitions out of
 * it are legal; the full machine is:</p>
 * <pre>
 *   CREATED -> COOKING | CANCELLED
 *   COOKING -> READY_FOR_DELIVERY | CANCELLED
 *   READY_FOR_DELIVERY -> DELIVERING | CANCELLED
 *   DELIVERING -> COMPLETED
 *   COMPLETED, CANCELLED  (terminal)
 * </pre>
 * <p>Keeping the map on the enum (instead of scattering {@code if} checks across
 * services) makes the lifecycle a single source of truth: both
 * {@link Order#changeStatus(OrderStatus)} and the web layer rely on it.</p>
 */
public enum OrderStatus {
    CREATED,
    COOKING,
    READY_FOR_DELIVERY,
    DELIVERING,
    COMPLETED,
    CANCELLED;

    /** Allowed successors per state. Terminal states map to an empty set. */
    private static final Set<OrderStatus> FROM_CREATED =
            Set.of(COOKING, CANCELLED);
    private static final Set<OrderStatus> FROM_COOKING =
            Set.of(READY_FOR_DELIVERY, CANCELLED);
    private static final Set<OrderStatus> FROM_READY =
            Set.of(DELIVERING, CANCELLED);
    private static final Set<OrderStatus> FROM_DELIVERING =
            Set.of(COMPLETED);
    private static final Set<OrderStatus> NONE = Set.of();

    /**
     * @param next the candidate next state
     * @return {@code true} if moving from this state to {@code next} is legal.
     */
    public boolean canTransitionTo(OrderStatus next) {
        return successors().contains(next);
    }

    private Set<OrderStatus> successors() {
        return switch (this) {
            case CREATED -> FROM_CREATED;
            case COOKING -> FROM_COOKING;
            case READY_FOR_DELIVERY -> FROM_READY;
            case DELIVERING -> FROM_DELIVERING;
            case COMPLETED, CANCELLED -> NONE;
        };
    }

    /** Human-readable Russian label for the UI. */
    public String labelRu() {
        return switch (this) {
            case CREATED -> "Создан";
            case COOKING -> "Готовится";
            case READY_FOR_DELIVERY -> "Готов к выдаче";
            case DELIVERING -> "В доставке";
            case COMPLETED -> "Доставлен";
            case CANCELLED -> "Отменён";
        };
    }
}
