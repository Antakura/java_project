package com.delivery.web.dto;

/** Client profile edit: username may be null (keep current), phone/address required. */
public record UpdateClientProfileRequest(String username, String phone, String address) {
}
