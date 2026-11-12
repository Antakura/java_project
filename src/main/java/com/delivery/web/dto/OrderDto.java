package com.delivery.web.dto;

import java.util.List;

/**
 * An order for the UI, with denormalized names (client, restaurant) so the
 * frontend does not need extra round-trips. Internal ids are kept — they are
 * needed to call status updates and history endpoints.
 */
public record OrderDto(long id, long clientId, String clientName,
                       long restaurantId, String restaurantName, Long courierId,
                       String status, String statusLabelRu, double totalPrice,
                       String deliveryAddress, String createdAt,
                       List<OrderItemDto> items) {
}
