package com.delivery.model;

/**
 * One line of an order: which product, how many, at what price.
 *
 * <p><b>Why this table exists (3NF):</b> the old design stored a plain list of
 * products on the order with no quantity and no price snapshot. Now each line fixes
 * {@code price_at_order} — the price at the moment of purchase — so later admin
 * price edits never rewrite history, and quantities are first-class data.</p>
 *
 * <p>The entity is an immutable value object created through the factory method
 * {@link #of(long, int, double)} (Factory Method pattern): invalid lines
 * (quantity &lt; 1, non-positive price) can never exist.</p>
 */
public class OrderItem {
    /** Assigned by the database on insert; 0 while the order is not persisted yet. */
    private final long id;
    /** FK -> orders.id (0 until the parent order is saved). */
    private final long orderId;
    /** FK -> products.id. */
    private final long productId;
    private final int quantity;
    /** Price of one unit fixed at the moment the order was placed. */
    private final double priceAtOrder;

    private OrderItem(long id, long orderId, long productId, int quantity, double priceAtOrder) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtOrder = priceAtOrder;
    }

    /**
     * Factory method for a new (not yet persisted) order line.
     */
    public static OrderItem of(long orderId, long productId, int quantity, double priceAtOrder) {
        if (productId <= 0) {
            throw new IllegalArgumentException("Order item must reference a product");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Order item quantity must be at least 1: " + quantity);
        }
        if (priceAtOrder <= 0) {
            throw new IllegalArgumentException(
                    "Price at order must be positive: " + priceAtOrder);
        }
        return new OrderItem(0, orderId, productId, quantity, priceAtOrder);
    }

    /** Rehydration factory used by the repository to restore persisted lines. */
    public static OrderItem rehydrate(long id, long orderId, long productId,
                                      int quantity, double priceAtOrder) {
        return new OrderItem(id, orderId, productId, quantity, priceAtOrder);
    }

    /** Total cost of this line (quantity x fixed unit price). */
    public double getLineTotal() {
        return quantity * priceAtOrder;
    }

    public long getId() {
        return id;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPriceAtOrder() {
        return priceAtOrder;
    }

    @Override
    public String toString() {
        return "OrderItem[product=" + productId + ", qty=" + quantity
                + ", price=" + priceAtOrder + "]";
    }
}
