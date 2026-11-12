package com.delivery.service;

import com.delivery.model.Product;
import com.delivery.model.Restaurant;
import com.delivery.model.Role;
import com.delivery.model.User;
import com.delivery.repository.ProductRepository;
import com.delivery.repository.RestaurantRepository;
import com.delivery.repository.UserRepository;

import java.util.List;

/**
 * Catalog service: restaurant/menu reads and admin product editing.
 *
 * <p>The specification requires "admin changes products by their id" — that is
 * {@link #updateProductAsAdmin(long, long, String, String, Double, Boolean)}.
 * Null arguments mean "leave this field as it is", so the UI can send partial
 * updates without re-reading the product first.</p>
 */
public class CatalogService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final ProductRepository productRepository;

    public CatalogService(UserRepository userRepository,
                          RestaurantRepository restaurantRepository,
                          ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.productRepository = productRepository;
    }

    /** All restaurants — the "Главная" page. */
    public List<Restaurant> getRestaurants() {
        return restaurantRepository.findAll();
    }

    /** The orderable menu of one restaurant (unavailable items hidden). */
    public List<Product> getMenu(long restaurantId) {
        ServiceGuards.requireRestaurant(restaurantRepository, restaurantId);
        return productRepository.findAvailableByRestaurantId(restaurantId);
    }

    /** Every product, including unavailable ones — the admin table. */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Admin-only partial update of a product by id.
     *
     * @param actorUserId  must be an ADMIN (checked here, not in the controller)
     * @param productId    which product to edit
     * @param name         new name, or {@code null} to keep it
     * @param description  new description, or {@code null} to keep it
     * @param price        new price, or {@code null} to keep it
     * @param isAvailable  new availability flag, or {@code null} to keep it
     */
    public Product updateProductAsAdmin(long actorUserId, long productId,
                                        String name, String description,
                                        Double price, Boolean isAvailable) {
        User actor = ServiceGuards.requireUser(userRepository, actorUserId);
        if (actor.getRole() != Role.ADMIN) {
            throw new IllegalStateException(
                    "Only an admin can edit products (user role: " + actor.getRole().getDbValue() + ")");
        }

        Product product = ServiceGuards.requireProduct(productRepository, productId);

        // Each mutator validates its own argument (encapsulation in the model).
        if (name != null && !name.isBlank()) {
            product.updateName(name);
        }
        if (description != null) {
            product.updateDescription(description);
        }
        if (price != null) {
            product.updatePrice(price);
        }
        if (isAvailable != null) {
            product.setAvailable(isAvailable);
        }

        productRepository.save(product);
        return product;
    }
}
