package com.delivery.web;

import com.delivery.repository.ClientRepository;
import com.delivery.repository.DistrictRepository;
import com.delivery.repository.ProductRepository;
import com.delivery.repository.RestaurantRepository;
import com.delivery.repository.UserRepository;
import com.delivery.service.CartService;
import com.delivery.service.CatalogService;
import com.delivery.service.CourierService;
import com.delivery.service.OrderService;
import com.delivery.service.UserService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;

import java.util.Map;

/**
 * Configures and starts the Javalin web server (presentation layer).
 *
 * <p><b>Layered Architecture:</b> this class only wires controllers to routes and
 * maps domain exceptions to HTTP codes. No business logic lives here — a controller
 * can be unit-tested without a running server, and the whole API can be replaced by
 * a CLI without touching services.</p>
 *
 * <p>The frontend (classpath {@code /public}) is mounted at "/", so the SPA is
 * available at http://localhost:7000/.</p>
 */
public class WebServer {
    private static final int PORT = 7000;

    private final Javalin app;

    public WebServer(UserService userService,
                     CatalogService catalogService,
                     CartService cartService,
                     OrderService orderService,
                     CourierService courierService,
                     UserRepository userRepository,
                     ClientRepository clientRepository,
                     RestaurantRepository restaurantRepository,
                     ProductRepository productRepository,
                     DistrictRepository districtRepository) {

        UserController userController = new UserController(userService, clientRepository);
        CatalogController catalogController = new CatalogController(
                catalogService, districtRepository, restaurantRepository);
        CartController cartController = new CartController(cartService, productRepository);
        OrderController orderController = new OrderController(
                orderService, clientRepository, userRepository,
                restaurantRepository, productRepository);
        CourierController courierController = new CourierController(
                courierService, districtRepository);

        this.app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            // Serve the SPA frontend (index.html) from classpath /public at "/".
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(PORT);

        // --- Users & client profile ---
        app.get("/api/users", userController::getAllUsers);
        app.get("/api/clients/{userId}", userController::getClientProfile);
        app.put("/api/clients/{userId}/profile", userController::updateProfile);

        // --- Catalog: restaurants, menus, districts, admin products ---
        app.get("/api/restaurants", catalogController::getRestaurants);
        app.get("/api/restaurants/{id}", catalogController::getRestaurantMenu);
        app.get("/api/products", catalogController::getAllProducts);
        app.get("/api/districts", catalogController::getDistricts);
        app.put("/api/products/{id}", catalogController::updateProduct);

        // --- Cart (split by restaurant) ---
        app.get("/api/cart/{userId}", cartController::getCart);
        app.post("/api/cart/{userId}/add", cartController::addToCart);
        app.delete("/api/cart/{userId}/item/{productId}", cartController::removeItem);
        app.delete("/api/cart/{userId}", cartController::clearCart);

        // --- Orders: checkout, history, status transitions ---
        app.get("/api/orders", orderController::getAllOrders);
        app.get("/api/orders/user/{userId}", orderController::getClientHistory);
        app.post("/api/orders", orderController::createOrder);
        app.post("/api/orders/{id}/status", orderController::updateStatus);

        // --- Couriers: district & shift ---
        app.get("/api/couriers/{userId}", courierController::getProfile);
        app.put("/api/couriers/{userId}/district", courierController::setDistrict);
        app.post("/api/couriers/{userId}/shift/start", courierController::startShift);
        app.post("/api/couriers/{userId}/shift/end", courierController::endShift);

        // --- Error handling: domain exceptions -> HTTP status codes ---
        // IllegalStateException  = business rule violated (conflict with current state)
        app.exception(IllegalStateException.class, (e, ctx) ->
                ctx.status(HttpStatus.CONFLICT).json(errorBody(e)));
        // IllegalArgumentException = bad input / unknown entity
        app.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(HttpStatus.BAD_REQUEST).json(errorBody(e)));
        // Malformed ids / missing numeric params (NumberFormatException is an
        // IllegalArgumentException, but Javalin picks the most specific handler).
        app.exception(NumberFormatException.class, (e, ctx) ->
                ctx.status(HttpStatus.BAD_REQUEST).json(errorBody(e)));

        System.out.println("[WebServer] Frontend available at http://localhost:" + PORT + "/");
    }

    /** Null-safe error payload: Map.of rejects null values. */
    private static Map<String, String> errorBody(Exception e) {
        String message = e.getMessage();
        return Map.of("error", message != null ? message : e.getClass().getSimpleName());
    }

    public void stop() {
        app.stop();
    }
}
