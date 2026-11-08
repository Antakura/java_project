package com.delivery.repository;

import com.delivery.model.Restaurant;
import java.util.Optional;

/**
 * Repository interface for managing restaurants.
 */
public interface RestaurantRepository extends Repository<Restaurant> {
    Optional<Restaurant> findById(String id);
}
