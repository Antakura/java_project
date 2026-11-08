package com.delivery.web;

import com.delivery.repository.ProductRepository;
import com.delivery.repository.RestaurantRepository;
import io.javalin.http.Context;

/**
 * Controller exposing menu data (products and restaurants) to the web client.
 */
public class ProductController {
    private final ProductRepository productRepository;
    private final RestaurantRepository restaurantRepository;

    public ProductController(ProductRepository productRepository, RestaurantRepository restaurantRepository) {
        this.productRepository = productRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public void getAllProducts(Context ctx) {
        ctx.json(productRepository.findAll());
    }

    public void getAllRestaurants(Context ctx) {
        ctx.json(restaurantRepository.findAll());
    }
}
