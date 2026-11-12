package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.*;
import com.delivery.strategy.FlatRateDeliveryStrategy;
import com.delivery.db.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CartServiceTest {

    private CartService cartService;
    private ProductRepository productRepository;
    private Client client;

    @BeforeEach
    void setUp() {
        // Use a unique in-memory DB for each test to avoid cross-contamination
        String dbUrl = "jdbc:sqlite::memory:" + java.util.UUID.randomUUID().toString();
        DatabaseManager db = new DatabaseManager(dbUrl);
        db.init();

        productRepository = new SqliteProductRepository(db);
        cartService = new CartService(); // Cart is in-memory ConcurrentHashMap

        // Seed some products
        productRepository.save(new Product("P1", "Pizza", 12.0, "R1"));
        productRepository.save(new Product("P2", "Coke", 2.5, "R1"));
        productRepository.save(new Product("P3", "Burger", 10.0, "R2"));

        client = new Client("C1", "Ivan", "+79991234567", "Moscow, Arbat St, 1");
    }

    @Test
    void splitCartGroupsItemsByRestaurant() {
        cartService.addToCart(client, productRepository.findById("P1").get());
        cartService.addToCart(client, productRepository.findById("P2").get());
        cartService.addToCart(client, productRepository.findById("P3").get());

        Map<String, List<Product>> split = cartService.getSplitCartByRestaurant(client.getId());

        assertEquals(2, split.size());
        assertEquals(2, split.get("R1").size());
        assertEquals(1, split.get("R2").size());
    }

    @Test
    void clearRestaurantItemsRemovesOnlyThatRestaurantsItems() {
        cartService.addToCart(client, productRepository.findById("P1").get());
        cartService.addToCart(client, productRepository.findById("P3").get());

        cartService.clearRestaurantItems(client.getId(), "R1");

        assertTrue(cartService.getItemsForRestaurant(client.getId(), "R1").isEmpty());
        assertEquals(1, cartService.getItemsForRestaurant(client.getId(), "R2").size());
    }
}
