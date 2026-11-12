package com.delivery.repository;

import com.delivery.model.Courier;

import java.util.Optional;

/**
 * Repository for {@code couriers} (1:1 profile of a user with role COURIER).
 */
public interface CourierRepository extends Repository<Courier> {

    /** Resolves the courier profile that belongs to the given login. */
    Optional<Courier> findByIdUser(long userId);
}
