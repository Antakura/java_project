package com.delivery.web.dto;

/**
 * Request body for changing an order's status.
 * The acting user (courier vs. client) is resolved on the server side:
 * transitions to DELIVERING/COMPLETED are performed by a Courier,
 * all other transitions — by the order's client.
 */
public record StatusUpdateRequest(
        String status
) {
}
