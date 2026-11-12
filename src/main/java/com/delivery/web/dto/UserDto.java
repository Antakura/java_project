package com.delivery.web.dto;

/**
 * Safe view of a user for the UI: deliberately NO password hash.
 * The domain {@code User} must never be serialized directly over HTTP.
 */
public record UserDto(long id, String username, String role) {
}
