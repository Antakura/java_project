package com.delivery.model;

/**
 * Represents the possible states of an Order.
 */
public enum OrderStatus {
    CREATED,
    COOKING,
    READY_FOR_DELIVERY,
    DELIVERING,
    COMPLETED,
    CANCELLED
}
