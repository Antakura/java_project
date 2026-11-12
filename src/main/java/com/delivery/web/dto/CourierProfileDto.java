package com.delivery.web.dto;

/** Courier profile for the "Страница пользователя" (courier variant). */
public record CourierProfileDto(long userId, long courierId, Long districtId,
                                String districtName, boolean onShift) {
}
