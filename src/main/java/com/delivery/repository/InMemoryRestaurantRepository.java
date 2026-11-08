package com.delivery.repository;

import com.delivery.model.Restaurant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of the restaurant repository.
 * Uses ConcurrentHashMap to ensure thread safety.
 */
public class InMemoryRestaurantRepository implements RestaurantRepository {
    private final Map<String, Restaurant> restaurants = new ConcurrentHashMap<>();

    @Override
    public void save(Restaurant restaurant) {
        restaurants.put(restaurant.getId(), restaurant);
    }

    @Override
    public Optional<Restaurant> findById(String id) {
        return Optional.ofNullable(restaurants.get(id));
    }

    @Override
    public List<Restaurant> findAll() {
        return new ArrayList<>(restaurants.values());
    }

    @Override
    public void delete(String id) {
        restaurants.remove(id);
    }
}
