package com.delivery.model;

import com.delivery.observer.OrderObserver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An order placed by a client for one restaurant.
 *
 * <h2>Patterns used here</h2>
 * <ul>
 *   <li><b>State pattern (tabular form):</b> {@link #changeStatus(OrderStatus)} validates
 *       every move against the transition map declared on {@link OrderStatus};
 *       illegal moves throw instead of silently corrupting state.</li>
 *   <li><b>Observer pattern:</b> the order is a Subject — status changes notify all
 *       registered {@link OrderObserver}s (console log, client/courier notifiers).</li>
 *   <li><b>Factory Method with two entry points</b> (see below) — this is also the
 *       core of the bug fix described next.</li>
 * </ul>
 *
 * <h2>Bug fix: "Cannot add products to an order that is already COOKING"</h2>
 * The old {@code SqliteOrderRepository.findById()} rebuilt a persisted order by
 * calling the business method {@code addProduct(...)} for every line and then forcing
 * the stored status through <i>reflection</i>. Since {@code addProduct} refuses to work
 * on any order that is not {@code CREATED}, reading an order back from the database
 * blew up as soon as it had advanced past {@code CREATED}.
 *
 * The fix: the constructor is private and there are exactly two ways to obtain an
 * {@code Order}:
 * <ul>
 *   <li>{@link #create(...)} — for brand-new orders; starts in {@code CREATED} and all
 *       subsequent mutations go through invariant-checked methods;</li>
 *   <li>{@link #rehydrate(...)} — a repository-only path that restores already-persisted
 *       state as-is, without re-running business rules. Persistence is not a business
 *       action, so it must not be subject to business invariants.</li>
 * </ul>
 * No reflection anywhere: the repository now calls {@code rehydrate} directly.
 */
public class Order {
    /** Assigned by the database on insert; 0 until persisted. */
    private long id;
    /** FK -> clients.id (the profile, not the login). */
    private final long clientId;
    /** FK -> restaurants.id: an order always belongs to exactly one restaurant. */
    private final long restaurantId;
    /** FK -> couriers.id, nullable until a courier takes the delivery. */
    private Long courierId;
    private OrderStatus status;
    /** Goods total + delivery fee, fixed at creation time. */
    private final double totalPrice;
    private final String deliveryAddress;
    private final Instant createdAt;
    private final List<OrderItem> items;
    /** Observer pattern: subjects keep their observers in a plain list. */
    private final List<OrderObserver> observers = new ArrayList<>();

    private Order(long id, long clientId, long restaurantId, Long courierId,
                  OrderStatus status, double totalPrice, String deliveryAddress,
                  Instant createdAt, List<OrderItem> items) {
        this.id = id;
        this.clientId = clientId;
        this.restaurantId = restaurantId;
        this.courierId = courierId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.deliveryAddress = deliveryAddress;
        this.createdAt = createdAt;
        this.items = new ArrayList<>(items);
    }

    /**
     * Factory for a <b>new</b> order: always starts in {@code CREATED}.
     */
    public static Order create(long clientId, long restaurantId, double totalPrice,
                               String deliveryAddress, List<OrderItem> items) {
        if (clientId <= 0 || restaurantId <= 0) {
            throw new IllegalArgumentException("Order must reference a client and a restaurant");
        }
        if (totalPrice < 0) {
            throw new IllegalArgumentException("Total price cannot be negative: " + totalPrice);
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw new IllegalArgumentException("Delivery address must not be blank");
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one item");
        }
        return new Order(0, clientId, restaurantId, null, OrderStatus.CREATED,
                totalPrice, deliveryAddress.trim(), Instant.now(), items);
    }

    /**
     * Factory for <b>rehydrating</b> a persisted order (repository use only).
     * Restores the stored state verbatim — no transition checks, no item guards —
     * because reading data back from storage is not a business operation.
     */
    public static Order rehydrate(long id, long clientId, long restaurantId, Long courierId,
                                  OrderStatus status, double totalPrice, String deliveryAddress,
                                  Instant createdAt, List<OrderItem> items) {
        return new Order(id, clientId, restaurantId, courierId, status,
                totalPrice, deliveryAddress, createdAt, items);
    }

    /** Storage-layer hook: records the generated primary key after INSERT. */
    public void assignId(long id) {
        if (this.id != 0) {
            throw new IllegalStateException("Order already has an id: " + this.id);
        }
        this.id = id;
    }

    /**
     * Adds a line while the order is still being composed.
     * Invariant: lines can only be added in {@code CREATED} state.
     */
    public void addItem(OrderItem item) {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot add items to an order that is already " + status);
        }
        items.add(item);
    }

    /**
     * State pattern: moves the order to {@code newStatus} if the transition map on
     * {@link OrderStatus} allows it, then notifies observers.
     */
    public void changeStatus(OrderStatus newStatus) {
        if (this.status == newStatus) {
            return; // no-op, keeps service code simple
        }
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Illegal status transition: " + this.status + " -> " + newStatus);
        }
        this.status = newStatus;
        notifyObservers();
    }

    /**
     * Binds a courier to the order (fills the nullable {@code id_courier} FK).
     * Invariants: once — and only while the order is not terminal.
     */
    public void assignCourier(long courierId) {
        if (courierId <= 0) {
            throw new IllegalArgumentException("Invalid courier id: " + courierId);
        }
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot assign a courier to a finished order");
        }
        if (this.courierId != null && this.courierId != courierId) {
            throw new IllegalStateException(
                    "Order is already assigned to courier " + this.courierId);
        }
        this.courierId = courierId;
    }

    // --- Observer pattern (Subject side) -------------------------------------

    public void addObserver(OrderObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    private void notifyObservers() {
        for (OrderObserver observer : observers) {
            observer.onStatusChanged(this);
        }
    }

    // --- Read access ----------------------------------------------------------

    public long getId() {
        return id;
    }

    public long getClientId() {
        return clientId;
    }

    public long getRestaurantId() {
        return restaurantId;
    }

    /** @return the assigned courier profile id, or {@code null} if not assigned yet. */
    public Long getCourierId() {
        return courierId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Unmodifiable view of the order lines. */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public String toString() {
        return "Order[id=" + id + ", client=" + clientId + ", restaurant=" + restaurantId
                + ", status=" + status + ", total=" + totalPrice + "]";
    }
}
