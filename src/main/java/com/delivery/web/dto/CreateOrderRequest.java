package com.delivery.web.dto;

/**
 * Checkout request: one restaurant's part of the cart becomes one order.
 * {@code userId} is the acting client's login id (the header selector value).
 */
public record CreateOrderRequest(long userId, long restaurantId, String deliveryAddress) {
}
