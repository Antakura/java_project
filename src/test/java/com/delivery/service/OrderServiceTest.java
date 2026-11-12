package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.*;
import com.delivery.strategy.FlatRateDeliveryStrategy;
import com.delivery.db.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private Client client;
    private Courier courier;
    private CartService cartService;
    private OrderService orderService;
    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private RestaurantRepository restaurantRepository;
    private DistrictRepository districtRepository;
    private CourierRepository courierRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        String dbUrl = "jdbc:sqlite::memory:" + java.util.UUID.randomUUID().toString();
        DatabaseManager db = new DatabaseManager(dbUrl);
        db.init();

        // Repositories
        productRepository = new SqliteProductRepository(db);
        restaurantRepository = new SqliteRestaurantRepository(db);
        districtRepository = new SqliteDistrictRepository(db);
        orderRepository = new SqliteOrderRepository(db);
        courierRepository = new SqliteCourierRepository(db);
        userRepository = new SqliteUserRepository(db);

        // Services & Helpers
        cartService = new CartService();
        List<com.delivery.service.OrderObserver> observers = new ArrayList<>(); // Empty list of observers for tests

        orderService = new OrderService(
                orderRepository,
                new FlatRateDeliveryStrategy(5.0),
                cartService,
                observers
        );

        // Seed Data
        District central = new District("D1", "Центральный");
        District suburb = new District("D2", "Пригородный");
        District outskirts = new District("D3", "Окраинный");
        districtRepository.save(central);
        districtRepository.save(suburb);
        districtRepository.save(outskirts);
        districtRepository.addNeighbor("D1", "D2");
        districtRepository.addNeighbor("D2", "D3");

        restaurantRepository.save(new Restaurant("R1", "Пицца Маргарита", "Адрес 1", "D1"));
        productRepository.save(new Product("P1", "Маргарита", 12.0, "R1"));
        productRepository.save(new Product("P2", "Пепперони", 15.0, "R1"));

        userRepository.save(new User("U1", "Ivan", "+79991234567", "Pass", "CLIENT"));
        client = new Client("U1", "Ivan", "+79991234567", "Moscow, Arbat St, 1");

        courierRepository.save(new Courier("CO1", "Alex", "+79997654321", "Bicycle", "D1", true));
        courier = new Courier("CO1", "Alex", "+79997654321", "Bicycle", "D1", true);
    }

    @Test
    void fullLifecycleTransitionsSucceed() {
        cartService.addToCart(client, productRepository.findById("P1").get());
        Order order = orderService.createOrderFromCart(client, "R1", "D1", "D2", 5.0);

        orderService.updateOrderStatus(order, OrderStatus.COOKING, client);
        orderService.updateOrderStatus(order, OrderStatus.READY_FOR_DELIVERY, client);
        orderService.updateOrderStatus(order, OrderStatus.DELIVERING, courier);
        orderService.updateOrderStatus(order, OrderStatus.COMPLETED, courier);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void skippingStepsIsRejected() {
        cartService.addToCart(client, productRepository.findById("P1").get());
        Order order = orderService.createOrderFromCart(client, "R1", "D1", "D2", 5.0);

        assertThrows(IllegalStateException.class, () ->
                orderService.updateOrderStatus(order, OrderStatus.COMPLETED, courier));
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void terminalStatesCannotBeChanged() {
        cartService.addToCart(client, productRepository.findById("P1").get());
        Order order = orderService.createOrderFromCart(client, "R1", "D1", "D2", 5.0);
        orderService.updateOrderStatus(order, OrderStatus.CANCELLED, client);

        assertThrows(IllegalStateException.class, () ->
                orderService.updateOrderStatus(order, OrderStatus.COMPLETED, courier));
    }

    @Test
    void onlyCourierCanStartDeliveryAndComplete() {
        cartService.addToCart(client, productRepository.findById("P1").get());
        Order order = orderService.createOrderFromCart(client, "R1", "D1", "D2", 5.0);
        orderService.updateOrderStatus(order, OrderStatus.COOKING, client);
        orderService.updateOrderStatus(order, OrderStatus.READY_FOR_DELIVERY, client);

        assertThrows(IllegalStateException.class, () ->
                orderService.updateOrderStatus(order, OrderStatus.DELIVERING, client));
    }

    @Test
    void regressionTestRehydrateBugFix() {
        cartService.addToCart(client, productRepository.findById("P1").get());
        Order order = orderService.createOrderFromCart(client, "R1", "D1", "D2", 5.0);

        // Advance to COOKING
        orderService.updateOrderStatus(order, OrderStatus.COOKING, client);

        // The original bug was that findById would return an order without products if status changed
        Order found = orderRepository.findById(order.getId());
        assertNotNull(found);
        assertEquals(OrderStatus.COOKING, found.getStatus());
        assertEquals(1, found.getProducts().size(), "Products should still be present in COOKING state");
    }

    @Test
    void cartIsSplitByRestaurantAndOnlyOrderedItemsAreCleared() {
        productRepository.save(new Product("P3", "Burger", 10.0, "R2"));
        restaurantRepository.save(new Restaurant("R2", "Burger House", "Адрес 2", "D2"));

        cartService.addToCart(client, productRepository.findById("P1").get());
        cartService.addToCart(client, productRepository.findById("P3").get());

        Order orderA = orderService.createOrderFromCart(client, "R1", "D1", "D2", 5.0);
        assertEquals(1, orderA.getProducts().size());

        // The cart is now empty for R1 but should still have P3 for R2.
        Order orderB = orderService.createOrderFromCart(client, "R2", "D2", "D1", 10.0);
        assertEquals(1, orderB.getProducts().size());

        // Attempting to order from R1 again should fail as it's empty in cart.
        assertThrows(IllegalStateException.class, () ->
                orderService.createOrderFromCart(client, "R1", "D1", "D2", 5.0));
    }
}
