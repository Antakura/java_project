package com.delivery.web.dto;

/**
 * Status change request. The actor is explicit in the body (no hidden session):
 * the service checks both the role and, for couriers, district eligibility.
 */
public record StatusUpdateRequest(String status, long actorId) {
}
