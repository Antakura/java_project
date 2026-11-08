package com.delivery.web.dto;

/**
 * Request body for creating an order from a client's cart.
 */
public record CreateOrderRequest(
        String clientId,
        String restaurantId,
        String fromDistrict,
        String toDistrict
) {
}
