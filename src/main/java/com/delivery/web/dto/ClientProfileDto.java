package com.delivery.web.dto;

/** Client profile as shown/edited on the "Страница пользователя". */
public record ClientProfileDto(long userId, long clientId, String username,
                               String phone, String defaultAddress) {
}
