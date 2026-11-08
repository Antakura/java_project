package com.delivery.service;

import com.delivery.model.Product;
import com.delivery.model.User;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for managing user shopping carts.
 * Handles the logic of splitting items by restaurant.
 * Thread-safe: backed by ConcurrentHashMap and CopyOnWriteArrayList,
 * consistent with the in-memory repositories.
 */
public class CartService {
    // Map of UserId -> List of Products in their cart
    private final Map<String, List<Product>> userCarts = new ConcurrentHashMap<>();

    public void addToCart(User user, Product product) {
        userCarts.computeIfAbsent(user.getId(), k -> new CopyOnWriteArrayList<>()).add(product);
    }

    public void clearCart(User user) {
        userCarts.remove(user.getId());
    }

    /**
     * Removes only the items of a specific restaurant from the user's cart,
     * leaving items of other restaurants intact. This allows splitting one
     * cart into several orders (one per restaurant).
     */
    public void clearRestaurantItems(String userId, String restaurantId) {
        List<Product> cart = userCarts.get(userId);
        if (cart != null) {
            cart.removeIf(product -> product.getRestaurantId().equals(restaurantId));
        }
    }

    /**
     * Splits the current cart into groups based on restaurantId.
     * Each group represents a potential order for a specific restaurant.
     * 
     * @param userId The ID of the user
     * @return A map where key is restaurantId and value is the list of products for that restaurant.
     */
    public Map<String, List<Product>> getSplitCartByRestaurant(String userId) {
        List<Product> cartItems = userCarts.getOrDefault(userId, Collections.emptyList());
        Map<String, List<Product>> splitCart = new HashMap<>();

        for (Product product : cartItems) {
            splitCart.computeIfAbsent(product.getRestaurantId(), k -> new ArrayList<>()).add(product);
        }
        return splitCart;
    }

    public List<Product> getItemsForRestaurant(String userId, String restaurantId) {
        return new ArrayList<>(getSplitCartByRestaurant(userId).getOrDefault(restaurantId, Collections.emptyList()));
    }
}
