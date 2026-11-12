package com.delivery.model;

/**
 * A product sold by a restaurant.
 *
 * <p><b>Mutability policy:</b> the specification requires the admin to modify products
 * by id, so business fields are mutable — but only through validating methods
 * (encapsulation). The identity ({@code id}) and the owning restaurant
 * ({@code restaurantId}) never change after creation.</p>
 */
public class Product {
    /** Assigned by the database on insert; 0 until persisted. */
    private long id;
    private String name;
    private String description;
    private double price;
    private String imageUrl;
    private boolean available;
    /** FK -> restaurants.id: immutable ownership. */
    private final long restaurantId;

    public Product(String name, String description, double price,
                   String imageUrl, boolean available, long restaurantId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Product price must be positive: " + price);
        }
        if (restaurantId <= 0) {
            throw new IllegalArgumentException("Product must belong to a restaurant");
        }
        this.name = name.trim();
        this.description = description == null ? "" : description.trim();
        this.price = price;
        this.imageUrl = imageUrl == null ? "" : imageUrl.trim();
        this.available = available;
        this.restaurantId = restaurantId;
    }

    public void assignId(long id) {
        if (this.id != 0) {
            throw new IllegalStateException("Product already has an id: " + this.id);
        }
        this.id = id;
    }

    /** Admin edit: rename the product. */
    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        this.name = name.trim();
    }

    /** Admin edit: change the description (may be empty). */
    public void updateDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    /** Admin edit: change the price. Must stay positive. */
    public void updatePrice(double price) {
        if (price <= 0) {
            throw new IllegalArgumentException("Product price must be positive: " + price);
        }
        this.price = price;
    }

    /** Admin edit: toggle availability (hidden from the menu when false). */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isAvailable() {
        return available;
    }

    public long getRestaurantId() {
        return restaurantId;
    }

    @Override
    public String toString() {
        return "Product[id=" + id + ", name=" + name + ", price=" + price + "]";
    }
}
