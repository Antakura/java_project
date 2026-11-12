package com.delivery.repository;

import com.delivery.model.Client;

import java.util.Optional;

/**
 * Repository for {@code clients} (1:1 profile of a user with role CLIENT).
 */
public interface ClientRepository extends Repository<Client> {

    /** Resolves the client profile that belongs to the given login. */
    Optional<Client> findByIdUser(long userId);
}
