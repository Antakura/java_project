package com.delivery.web;

import com.delivery.model.Courier;
import com.delivery.repository.ProductRepository;
import com.delivery.repository.RestaurantRepository;
import com.delivery.service.CartService;
import com.delivery.service.OrderService;
import com.delivery.service.UserService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * Configures and starts the Javalin web server.
 * Static files (the frontend) are served from classpath /public by default,
 * so index.html is available at http://localhost:7000/
 */
public class WebServer {
    private static final int PORT = 7000;

    private final Javalin app;

    public WebServer(UserService userService,
                     ProductRepository productRepository,
                     RestaurantRepository restaurantRepository,
                     CartService cartService,
                     OrderService orderService,
                     Courier courierActor) {

        UserController userController = new UserController(userService);
        ProductController productController = new ProductController(productRepository, restaurantRepository);
        CartController cartController = new CartController(cartService, userService, productRepository);
        OrderController orderController = new OrderController(orderService, userService, courierActor);

        this.app = Javalin.create(config -> config.showJavalinBanner = false).start(PORT);

        // --- Users ---
        app.get("/users", userController::getAllUsers);
        app.get("/users/{id}", userController::getUser);
        app.post("/users", userController::createUser);

        // --- Menu (products & restaurants) ---
        app.get("/api/products", productController::getAllProducts);
        app.get("/api/restaurants", productController::getAllRestaurants);

        // --- Cart ---
        app.get("/api/cart/{userId}", cartController::getCart);
        app.post("/api/cart/{userId}/add", cartController::addToCart);
        app.delete("/api/cart/{userId}", cartController::clearCart);

        // --- Orders ---
        app.get("/api/orders", orderController::getAllOrders);
        app.post("/api/orders", orderController::createOrder);
        app.post("/api/orders/{id}/status", orderController::updateStatus);

        // --- Error handling: map domain exceptions to HTTP status codes ---
        app.exception(IllegalStateException.class, (e, ctx) ->
                ctx.status(HttpStatus.CONFLICT).json(java.util.Map.of("error", e.getMessage())));
        app.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(HttpStatus.BAD_REQUEST).json(java.util.Map.of("error", e.getMessage())));

        System.out.println("[WebServer] Frontend available at http://localhost:" + PORT + "/");
    }

    public void stop() {
        app.stop();
    }
}
