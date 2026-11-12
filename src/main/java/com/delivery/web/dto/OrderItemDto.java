package com.delivery.web.dto;

/** One order line: the price is the one frozen at purchase time. */
public record OrderItemDto(long productId, String name, int quantity, double priceAtOrder) {
}
