package com.delivery.repository;

import com.delivery.model.Product;

import java.util.List;

/**
 * Repository for {@code products}.
 */
public interface ProductRepository extends Repository<Product> {

    /** All products of a restaurant, including unavailable ones (admin view). */
    List<Product> findByRestaurantId(long restaurantId);

    /** Only the products that can currently be ordered (menu page). */
    List<Product> findAvailableByRestaurantId(long restaurantId);
}
