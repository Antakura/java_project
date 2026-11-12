package com.delivery.service;

import com.delivery.model.CartLine;
import com.delivery.model.Product;
import com.delivery.repository.ProductRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shopping cart service.
 *
 * <h2>Key requirement: one cart, many baskets</h2>
 * A user has exactly ONE cart (keyed by user id), but items from different
 * restaurants must be checked out as SEPARATE orders. That is why the main read
 * method is {@link #getSplitByRestaurant(long)} — it groups the lines by the
 * owning restaurant, and checkout operates on one group at a time.
 *
 * <h2>Why only ids in memory?</h2>
 * Lines are {@code (productId, quantity)} value objects; prices are re-read from
 * the catalog at checkout and frozen into {@code order_items.price_at_order}.
 * The cart therefore never goes stale and stays trivially thread-safe.
 */
public class CartService {

    /** userId -> productId -> line. ConcurrentHashMap: web handlers run on pool threads. */
    private final Map<Long, Map<Long, CartLine>> carts = new ConcurrentHashMap<>();

    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** Adds one unit of a product (creates the line if it is not there yet). */
    public void addToCart(long userId, long productId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user id: " + userId);
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (!product.isAvailable()) {
            throw new IllegalStateException("Product is not available: " + product.getName());
        }
        carts.computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .compute(productId, (id, line) -> line == null ? new CartLine(id, 1) : line.increment());
    }

    /** Removes a whole line from the cart. No-op if the product is not in it. */
    public void removeItem(long userId, long productId) {
        Map<Long, CartLine> cart = carts.get(userId);
        if (cart != null) {
            cart.remove(productId);
        }
    }

    /** Empties the entire cart of a user. */
    public void clearCart(long userId) {
        carts.remove(userId);
    }

    /**
     * The "split basket" view: lines grouped by restaurant id.
     * Restaurant order is stable (insertion order) so the UI renders sections
     * in a predictable way.
     */
    public Map<Long, List<CartLine>> getSplitByRestaurant(long userId) {
        Map<Long, List<CartLine>> split = new LinkedHashMap<>();
        Map<Long, CartLine> cart = carts.get(userId);
        if (cart == null) {
            return split;
        }
        for (CartLine line : cart.values()) {
            Product product = productRepository.findById(line.getProductId()).orElse(null);
            // A deleted product leaves a stale line: skip it in the view.
            if (product == null) {
                continue;
            }
            split.computeIfAbsent(product.getRestaurantId(), key -> new ArrayList<>())
                    .add(line);
        }
        return split;
    }

    /** Lines of one restaurant only — what checkout consumes. */
    public List<CartLine> getLinesForRestaurant(long userId, long restaurantId) {
        return getSplitByRestaurant(userId).getOrDefault(restaurantId, List.of());
    }

    /** After a successful checkout: drop exactly that restaurant's lines. */
    public void clearRestaurant(long userId, long restaurantId) {
        Map<Long, CartLine> cart = carts.get(userId);
        if (cart == null) {
            return;
        }
        cart.entrySet().removeIf(entry -> {
            Product product = productRepository.findById(entry.getKey()).orElse(null);
            return product != null && product.getRestaurantId() == restaurantId;
        });
    }
}
