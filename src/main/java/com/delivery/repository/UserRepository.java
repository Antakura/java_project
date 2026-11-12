package com.delivery.repository;

import com.delivery.model.User;

import java.util.Optional;

/**
 * Repository for {@code users} (login, password hash, role).
 */
public interface UserRepository extends Repository<User> {

    /** Looks a user up by the unique login name. */
    Optional<User> findByUsername(String username);
}
