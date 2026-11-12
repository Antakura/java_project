package com.delivery.web.dto;

/** A restaurant card on the "Главная" page, with its district name resolved. */
public record RestaurantDto(long id, String name, String address,
                            long districtId, String districtName) {
}
