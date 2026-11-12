package com.delivery.model;

/**
 * One line of a shopping cart: product + quantity.
 *
 * <p>The cart deliberately stores only the product id and a count, not live
 * {@link Product} objects: prices are re-read from the catalog at checkout time and
 * fixed into {@code order_items.price_at_order}. This keeps the cart small, thread-safe
 * and immune to stale-price bugs.</p>
 */
public class CartLine {
    private final long productId;
    private final int quantity;

    public CartLine(long productId, int quantity) {
        if (productId <= 0) {
            throw new IllegalArgumentException("Cart line must reference a product");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Cart line quantity must be at least 1: " + quantity);
        }
        this.productId = productId;
        this.quantity = quantity;
    }

    /** Returns a copy with the quantity increased by one (immutability). */
    public CartLine increment() {
        return new CartLine(productId, quantity + 1);
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public String toString() {
        return "CartLine[product=" + productId + ", qty=" + quantity + "]";
    }
}
