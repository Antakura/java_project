package com.delivery.service;

import com.delivery.db.DatabaseManager;
import com.delivery.model.Client;
import com.delivery.model.Courier;
import com.delivery.model.District;
import com.delivery.model.Order;
import com.delivery.model.OrderStatus;
import com.delivery.model.Product;
import com.delivery.model.Restaurant;
import com.delivery.model.Role;
import com.delivery.model.User;
import com.delivery.observer.OrderObserver;
import com.delivery.repository.ClientRepository;
import com.delivery.repository.CourierRepository;
import com.delivery.repository.OrderRepository;
import com.delivery.repository.ProductRepository;
import com.delivery.repository.RestaurantRepository;
import com.delivery.repository.UserRepository;
import com.delivery.repository.impl.SqliteClientRepository;
import com.delivery.repository.impl.SqliteCourierRepository;
import com.delivery.repository.impl.SqliteDistrictRepository;
import com.delivery.repository.impl.SqliteOrderRepository;
import com.delivery.repository.impl.SqliteProductRepository;
import com.delivery.repository.impl.SqliteRestaurantRepository;
import com.delivery.repository.impl.SqliteUserRepository;
import com.delivery.strategy.FlatRateDeliveryStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceTest {

    private static final String ADDRESS = "Moscow, Arbat St, 1";

    private CartService cartService;
    private OrderService orderService;
    private OrderRepository orderRepository;
    private UserRepository userRepository;
    private ClientRepository clientRepository;
    private CourierRepository courierRepository;
    private SqliteDistrictRepository districtRepository;
    private RestaurantRepository restaurantRepository;
    private ProductRepository productRepository;

    private User clientUser;
    private User courierUser;
    private User adminUser;
    private Courier courier;
    private Restaurant pizzaPlace;
    private Restaurant burgerPlace;
    private Product margherita;
    private Product burger;

    @BeforeEach
    void setUp() {
        // Each test gets a fresh connection, i.e. its own private in-memory database.
        DatabaseManager db = new DatabaseManager("jdbc:sqlite::memory:");

        UserRepository userRepository = new SqliteUserRepository(db.getConnection());
        ClientRepository clientRepository = new SqliteClientRepository(db.getConnection());
        CourierRepository courierRepository = new SqliteCourierRepository(db.getConnection());
        SqliteDistrictRepository districtRepository = new SqliteDistrictRepository(db.getConnection());
        RestaurantRepository restaurantRepository = new SqliteRestaurantRepository(db.getConnection());
        ProductRepository productRepository = new SqliteProductRepository(db.getConnection());
        orderRepository = new SqliteOrderRepository(db.getConnection());
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.courierRepository = courierRepository;
        this.districtRepository = districtRepository;
        this.restaurantRepository = restaurantRepository;
        this.productRepository = productRepository;

        cartService = new CartService(productRepository);
        List<OrderObserver> observers = List.of(); // no notifications in tests
        orderService = new OrderService(
                orderRepository, clientRepository, productRepository, restaurantRepository,
                districtRepository, courierRepository, userRepository, cartService,
                new FlatRateDeliveryStrategy(5.0), observers);

        // District chain: Центральный — Пригородный — Окраинный (symmetric edges).
        District central = new District("Центральный");
        District suburb = new District("Пригородный");
        District outskirts = new District("Окраинный");
        districtRepository.save(central);
        districtRepository.save(suburb);
        districtRepository.save(outskirts);
        districtRepository.addNeighbor(central.getId(), suburb.getId());
        districtRepository.addNeighbor(suburb.getId(), central.getId());
        districtRepository.addNeighbor(suburb.getId(), outskirts.getId());
        districtRepository.addNeighbor(outskirts.getId(), suburb.getId());

        pizzaPlace = new Restaurant("Пицца Маргарита", "Адрес 1", central.getId());
        burgerPlace = new Restaurant("Burger House", "Адрес 2", suburb.getId());
        restaurantRepository.save(pizzaPlace);
        restaurantRepository.save(burgerPlace);

        margherita = new Product("Маргарита", "", 12.0, "", true, pizzaPlace.getId());
        Product pepperoni = new Product("Пепперони", "", 15.0, "", true, pizzaPlace.getId());
        burger = new Product("Burger", "", 10.0, "", true, burgerPlace.getId());
        productRepository.save(margherita);
        productRepository.save(pepperoni);
        productRepository.save(burger);

        adminUser = new User("admin", "hash", Role.ADMIN);
        userRepository.save(adminUser);

        clientUser = new User("Ivan", "hash", Role.CLIENT);
        userRepository.save(clientUser);
        clientRepository.save(new Client(clientUser.getId(), "+79991234567", ADDRESS));

        courierUser = new User("Alex", "hash", Role.COURIER);
        userRepository.save(courierUser);
        courier = new Courier(courierUser.getId());
        courier.changeDistrict(central.getId());
        courier.startShift();
        courierRepository.save(courier);
    }

    private Order orderFromCart(Restaurant restaurant) {
        return orderService.createOrderFromCart(clientUser.getId(), restaurant.getId(), ADDRESS);
    }

    @Test
    void fullLifecycleTransitionsSucceed() {
        cartService.addToCart(clientUser.getId(), margherita.getId());
        Order order = orderFromCart(pizzaPlace);
        assertEquals(17.0, order.getTotalPrice(), 0.001); // 12.0 goods + 5.0 flat fee

        orderService.updateStatus(order.getId(), OrderStatus.COOKING, adminUser.getId());
        orderService.updateStatus(order.getId(), OrderStatus.READY_FOR_DELIVERY, adminUser.getId());
        orderService.updateStatus(order.getId(), OrderStatus.DELIVERING, courierUser.getId());
        Order completed = orderService.updateStatus(
                order.getId(), OrderStatus.COMPLETED, courierUser.getId());

        assertEquals(OrderStatus.COMPLETED, completed.getStatus());
        assertEquals(courier.getId(), completed.getCourierId().longValue());
    }

    @Test
    void skippingStepsIsRejected() {
        cartService.addToCart(clientUser.getId(), margherita.getId());
        Order order = orderFromCart(pizzaPlace);
        orderService.updateStatus(order.getId(), OrderStatus.COOKING, adminUser.getId());

        // COOKING -> DELIVERING skips READY_FOR_DELIVERY.
        assertThrows(IllegalStateException.class, () ->
                orderService.updateStatus(order.getId(), OrderStatus.DELIVERING, courierUser.getId()));
        assertEquals(OrderStatus.COOKING,
                orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void terminalStatesCannotBeChanged() {
        cartService.addToCart(clientUser.getId(), margherita.getId());
        Order order = orderFromCart(pizzaPlace);
        orderService.updateStatus(order.getId(), OrderStatus.CANCELLED, clientUser.getId());

        assertThrows(IllegalStateException.class, () ->
                orderService.updateStatus(order.getId(), OrderStatus.COMPLETED, courierUser.getId()));
    }

    @Test
    void onlyCourierCanStartDeliveryAndComplete() {
        cartService.addToCart(clientUser.getId(), margherita.getId());
        Order order = orderFromCart(pizzaPlace);
        orderService.updateStatus(order.getId(), OrderStatus.COOKING, adminUser.getId());
        orderService.updateStatus(order.getId(), OrderStatus.READY_FOR_DELIVERY, adminUser.getId());

        assertThrows(IllegalStateException.class, () ->
                orderService.updateStatus(order.getId(), OrderStatus.DELIVERING, clientUser.getId()));
        assertThrows(IllegalStateException.class, () ->
                orderService.updateStatus(order.getId(), OrderStatus.DELIVERING, adminUser.getId()));
    }

    @Test
    void regressionTestRehydrateBugFix() {
        cartService.addToCart(clientUser.getId(), margherita.getId());
        Order order = orderFromCart(pizzaPlace);

        orderService.updateStatus(order.getId(), OrderStatus.COOKING, adminUser.getId());

        // The original bug: findById returned an order without items once status advanced.
        Order found = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.COOKING, found.getStatus());
        assertEquals(1, found.getItems().size(), "Items should still be present in COOKING state");
    }

    @Test
    void cartIsSplitByRestaurantAndOnlyOrderedItemsAreCleared() {
        cartService.addToCart(clientUser.getId(), margherita.getId());
        cartService.addToCart(clientUser.getId(), burger.getId());

        Order orderA = orderFromCart(pizzaPlace);
        assertEquals(1, orderA.getItems().size());

        // The cart is now empty for the pizza place but still holds the burger.
        Order orderB = orderFromCart(burgerPlace);
        assertEquals(1, orderB.getItems().size());

        // Ordering from the pizza place again fails: its part of the cart is empty.
        assertThrows(IllegalArgumentException.class, () -> orderFromCart(pizzaPlace));
    }

    @Test
    void observersAreNotifiedForOrdersLoadedFromStorage() {
        List<OrderStatus> seen = new java.util.ArrayList<>();
        OrderObserver recorder = order -> seen.add(order.getStatus());
        OrderService service = new OrderService(
                orderRepository, clientRepository, productRepository, restaurantRepository,
                districtRepository, courierRepository, userRepository, cartService,
                new FlatRateDeliveryStrategy(5.0), List.of(recorder));

        cartService.addToCart(clientUser.getId(), margherita.getId());
        Order order = service.createOrderFromCart(clientUser.getId(), pizzaPlace.getId(), ADDRESS);

        // The order is re-read from the database on every status change: observers
        // must be re-attached there, otherwise persisted orders notify nobody.
        service.updateStatus(order.getId(), OrderStatus.COOKING, adminUser.getId());
        assertEquals(List.of(OrderStatus.COOKING), seen);
    }

    @Test
    void updatesSucceedWhenChildRowsReferenceTheEntity() {
        cartService.addToCart(clientUser.getId(), margherita.getId());
        Order order = orderFromCart(pizzaPlace);
        orderService.updateStatus(order.getId(), OrderStatus.COOKING, adminUser.getId());
        orderService.updateStatus(order.getId(), OrderStatus.READY_FOR_DELIVERY, adminUser.getId());
        orderService.updateStatus(order.getId(), OrderStatus.DELIVERING, courierUser.getId());

        // orders.id_client -> clients: editing the profile must not break the FK.
        UserService userService = new UserService(userRepository, clientRepository);
        Client updated = userService.updateClientProfile(
                clientUser.getId(), null, "+7 999 000-00-00", "New address, 1");
        assertEquals("New address, 1", updated.getDefaultAddress());

        // orders.id_courier -> couriers: ending the shift must persist.
        CourierService courierService = new CourierService(
                userRepository, courierRepository, districtRepository);
        assertFalse(courierService.endShift(courierUser.getId()).isOnShift());

        // order_items.id_product -> products: admin price edit must persist.
        CatalogService catalogService = new CatalogService(
                userRepository, restaurantRepository, productRepository);
        Product edited = catalogService.updateProductAsAdmin(
                adminUser.getId(), margherita.getId(), null, null, 99.0, null);
        assertEquals(99.0, edited.getPrice(), 0.001);

        // The order itself must still be readable with its items intact.
        Order found = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.DELIVERING, found.getStatus());
        assertEquals(1, found.getItems().size());
    }
}