package com.delivery.web.dto;

/** One cart line with the live catalog price resolved for display. */
public record CartLineDto(long productId, String name, double price,
                          int quantity, double lineTotal) {
}
