package com.delivery.web;

import com.delivery.model.Product;
import com.delivery.service.CartService;
import com.delivery.repository.ProductRepository;
import com.delivery.service.UserService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

/**
 * Controller for managing the shopping cart of a client.
 */
public class CartController {
    private final CartService cartService;
    private final UserService userService;
    private final ProductRepository productRepository;

    public CartController(CartService cartService, UserService userService, ProductRepository productRepository) {
        this.cartService = cartService;
        this.userService = userService;
        this.productRepository = productRepository;
    }

    /** POST /api/cart/{userId}/add  body: {"productId": "..."} */
    public void addToCart(Context ctx) {
        String userId = ctx.pathParam("userId");
        var userOpt = userService.getUser(userId);
        if (userOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("User not found: " + userId);
            return;
        }

        String productId = ctx.bodyAsClass(java.util.Map.class)
                .getOrDefault("productId", "").toString();
        var productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Product not found: " + productId);
            return;
        }

        cartService.addToCart(userOpt.get(), productOpt.get());
        ctx.status(HttpStatus.CREATED).json(cartService.getSplitCartByRestaurant(userId));
    }

    /** GET /api/cart/{userId} */
    public void getCart(Context ctx) {
        String userId = ctx.pathParam("userId");
        ctx.json(cartService.getSplitCartByRestaurant(userId));
    }

    /** DELETE /api/cart/{userId} */
    public void clearCart(Context ctx) {
        String userId = ctx.pathParam("userId");
        var userOpt = userService.getUser(userId);
        if (userOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("User not found: " + userId);
            return;
        }
        cartService.clearCart(userOpt.get());
        ctx.status(HttpStatus.NO_CONTENT);
    }
}
