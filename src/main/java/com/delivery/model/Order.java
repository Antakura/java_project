package com.delivery.model;

import com.delivery.observer.OrderObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an order in the system.
 * Implements the Subject part of the Observer pattern.
 */
public class Order {
    private final String id;
    private final Client client;
    private final String restaurantId;
    private final District fromDistrict;
    private final District toDistrict;
    private final List<Product> products;
    private OrderStatus status;
    private final double distanceKm;
    private final List<OrderObserver> observers;

    public Order(String id, Client client, String restaurantId, District fromDistrict, District toDistrict, double distanceKm) {
        this.id = id;
        this.client = client;
        this.restaurantId = restaurantId;
        this.fromDistrict = fromDistrict;
        this.toDistrict = toDistrict;
        this.products = new ArrayList<>();
        this.status = OrderStatus.CREATED;
        this.distanceKm = distanceKm;
        this.observers = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public District getFromDistrict() {
        return fromDistrict;
    }

    public District getToDistrict() {
        return toDistrict;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public List<Product> getProducts() {
        return Collections.unmodifiableList(products);
    }

    public void addProduct(Product product) {
        if (status == OrderStatus.CREATED) {
            products.add(product);
        } else {
            throw new IllegalStateException("Cannot add products to an order that is already " + status);
        }
    }

    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Updates the order status and notifies all observers.
     */
    public void updateStatus(OrderStatus newStatus) {
        if (this.status == newStatus) return;
        
        if (this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot change status of a finished order.");
        }

        this.status = newStatus;
        notifyObservers();
    }

    public void addObserver(OrderObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        for (OrderObserver observer : observers) {
            observer.onStatusChanged(this);
        }
    }

    public double getTotalPrice() {
        return products.stream().mapToDouble(Product::getPrice).sum();
    }

    @Override
    public String toString() {
        return String.format("Order[id=%s, client=%s, restaurant=%s, status=%s, total=%.2f]", 
                id, client.getName(), restaurantId, status, getTotalPrice());
    }
}
