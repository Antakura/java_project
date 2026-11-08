package com.delivery.service;

import com.delivery.model.Client;
import com.delivery.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartServiceTest {

    private Client client;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        client = new Client("C1", "Ivan", "+79991234567", "Moscow, Arbat St, 1");
        cartService = new CartService();
    }

    @Test
    void splitCartGroupsItemsByRestaurant() {
        cartService.addToCart(client, new Product("P1", "Pizza", 12.0, "R1"));
        cartService.addToCart(client, new Product("P2", "Coke", 2.5, "R1"));
        cartService.addToCart(client, new Product("P3", "Burger", 10.0, "R2"));

        Map<String, List<Product>> split = cartService.getSplitCartByRestaurant(client.getId());

        assertEquals(2, split.size());
        assertEquals(2, split.get("R1").size());
        assertEquals(1, split.get("R2").size());
    }

    @Test
    void clearRestaurantItemsRemovesOnlyThatRestaurantsItems() {
        cartService.addToCart(client, new Product("P1", "Pizza", 12.0, "R1"));
        cartService.addToCart(client, new Product("P3", "Burger", 10.0, "R2"));

        cartService.clearRestaurantItems(client.getId(), "R1");

        assertTrue(cartService.getItemsForRestaurant(client.getId(), "R1").isEmpty());
        assertEquals(1, cartService.getItemsForRestaurant(client.getId(), "R2").size());
    }
}
