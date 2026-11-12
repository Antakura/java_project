package com.delivery.web.dto;

/**
 * Partial product update from the admin table: any field may be {@code null}
 * ("leave as is"). Jackson maps missing JSON fields to null on records.
 */
public record UpdateProductRequest(String name, String description,
                                   Double price, Boolean isAvailable) {
}
