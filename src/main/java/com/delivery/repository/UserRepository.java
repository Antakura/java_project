package com.delivery.repository;

import com.delivery.model.User;
import java.util.Optional;

public interface UserRepository extends Repository<User> {
    Optional<User> findById(String id);
}
