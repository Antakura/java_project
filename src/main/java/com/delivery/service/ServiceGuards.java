package com.delivery.service;

import com.delivery.model.Client;
import com.delivery.model.Courier;
import com.delivery.model.Product;
import com.delivery.model.Restaurant;
import com.delivery.model.Role;
import com.delivery.model.User;
import com.delivery.repository.ClientRepository;
import com.delivery.repository.CourierRepository;
import com.delivery.repository.ProductRepository;
import com.delivery.repository.RestaurantRepository;
import com.delivery.repository.UserRepository;

/**
 * Shared "find-or-throw" lookups and the generic role check (DRY).
 *
 * <p>These guards used to be copy-pasted across the services (and a couple of
 * controllers). Centralizing them keeps the exception <i>types</i> and messages
 * consistent: missing input/unknown entity → {@link IllegalArgumentException}
 * (HTTP 400), violated business rule → {@link IllegalStateException} (HTTP 409),
 * which is exactly what {@code WebServer} maps to status codes.</p>
 */
public final class ServiceGuards {

    private ServiceGuards() {
    }

    /** @throws IllegalArgumentException if the user does not exist. */
    public static User requireUser(UserRepository users, long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    /** @throws IllegalStateException if the user does not hold the expected role. */
    public static void requireRole(User user, Role expected, String action) {
        if (user.getRole() != expected) {
            throw new IllegalStateException(
                    "User " + user.getUsername() + " (" + user.getRole().getDbValue()
                            + ") cannot " + action);
        }
    }

    /** @throws IllegalStateException if the user has no courier profile. */
    public static Courier requireCourierProfile(CourierRepository couriers, long userId) {
        return couriers.findByIdUser(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Courier profile not found for user " + userId));
    }

    /** @throws IllegalStateException if the user has no client profile. */
    public static Client requireClientProfile(ClientRepository clients, long userId) {
        return clients.findByIdUser(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Client profile not found for user " + userId));
    }

    /** @throws IllegalArgumentException if the restaurant does not exist. */
    public static Restaurant requireRestaurant(RestaurantRepository restaurants, long restaurantId) {
        return restaurants.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
    }

    /** @throws IllegalArgumentException if the product does not exist. */
    public static Product requireProduct(ProductRepository products, long productId) {
        return products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }
}