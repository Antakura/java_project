package com.delivery.web.dto;

/** A product as shown in a menu or in the admin table. */
public record ProductDto(long id, String name, String description, double price,
                         String imageUrl, boolean isAvailable, long restaurantId,
                         String restaurantName) {
}
