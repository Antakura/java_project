package com.delivery;

import com.delivery.db.DatabaseManager;
import com.delivery.model.Client;
import com.delivery.model.Courier;
import com.delivery.model.District;
import com.delivery.model.Product;
import com.delivery.model.Restaurant;
import com.delivery.model.Role;
import com.delivery.model.User;
import com.delivery.observer.ClientNotifier;
import com.delivery.observer.ConsoleOrderObserver;
import com.delivery.observer.CourierNotifier;
import com.delivery.observer.OrderObserver;
import com.delivery.repository.ClientRepository;
import com.delivery.repository.CourierRepository;
import com.delivery.repository.DistrictRepository;
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
import com.delivery.service.CartService;
import com.delivery.service.CatalogService;
import com.delivery.service.CourierService;
import com.delivery.service.OrderService;
import com.delivery.service.UserService;
import com.delivery.strategy.DeliveryCostStrategy;
import com.delivery.strategy.DeliveryCostStrategyFactory;
import com.delivery.web.WebServer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Composition root of the application.
 *
 * <p><b>Layered Architecture:</b> this is the ONLY place where concrete classes are
 * wired together (dependency injection by constructor). Every other layer depends on
 * abstractions: services depend on repository interfaces, controllers depend on
 * services, and the web server depends on everything above. Swapping SQLite for any
 * other storage means changing only this file.</p>
 *
 * <p><b>Patterns wired here:</b></p>
 * <ul>
 *   <li><b>Strategy + Factory Method:</b> the delivery fee policy is created by
 *       {@link DeliveryCostStrategyFactory} and injected into {@code OrderService};</li>
 *   <li><b>Observer:</b> all observers (console log, client notifier, courier
 *       notifier) are registered once and attached to every new order;</li>
 *   <li><b>Repository:</b> concrete SQLite implementations stand in for the
 *       repository interfaces used by services.</li>
 * </ul>
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Starting NyamNyam System...");

        // 1. Persistence: one shared SQLite connection, schema auto-migrated to v2.
        DatabaseManager db = new DatabaseManager();

        // 2. Repositories (concrete implementations behind interfaces).
        UserRepository userRepository = new SqliteUserRepository(db.getConnection());
        ClientRepository clientRepository = new SqliteClientRepository(db.getConnection());
        CourierRepository courierRepository = new SqliteCourierRepository(db.getConnection());
        // The concrete class is kept in a variable because addNeighbor() (adjacency
        // graph edges) is a storage-level operation not part of the domain interface.
        SqliteDistrictRepository districtRepository = new SqliteDistrictRepository(db.getConnection());
        RestaurantRepository restaurantRepository = new SqliteRestaurantRepository(db.getConnection());
        ProductRepository productRepository = new SqliteProductRepository(db.getConnection());
        OrderRepository orderRepository = new SqliteOrderRepository(db.getConnection());

        // 3. Services (business logic, depend on repository interfaces only).
        CartService cartService = new CartService(productRepository);

        // Strategy + Factory Method: "free delivery above 2000 rubles, otherwise 150".
        DeliveryCostStrategyFactory strategyFactory =
                new DeliveryCostStrategyFactory(150.0, 150.0, 2000.0);
        DeliveryCostStrategy deliveryFee =
                strategyFactory.create(DeliveryCostStrategyFactory.Mode.THRESHOLD_FREE);

        // Observer pattern: every status change of a new order is fanned out to all
        // registered observers (console + client/courier notifications).
        List<OrderObserver> observers = List.of(
                new ConsoleOrderObserver(),
                new ClientNotifier(),
                new CourierNotifier());

        OrderService orderService = new OrderService(
                orderRepository, clientRepository, productRepository, restaurantRepository,
                districtRepository, courierRepository, userRepository, cartService,
                deliveryFee, observers);
        UserService userService = new UserService(userRepository, clientRepository);
        CourierService courierService = new CourierService(
                userRepository, courierRepository, districtRepository);
        CatalogService catalogService = new CatalogService(
                userRepository, restaurantRepository, productRepository);

        // 4. Demo data (idempotent: skipped when the database is already populated).
        seedIfEmpty(userRepository, clientRepository, courierRepository,
                districtRepository, restaurantRepository, productRepository);

        // 5. Presentation layer: REST API + SPA frontend on port 7000.
        WebServer webServer = new WebServer(
                userService, catalogService, cartService, orderService, courierService,
                userRepository, clientRepository, restaurantRepository,
                productRepository, districtRepository);

        System.out.println("System is running. Frontend: http://localhost:7000/");
        System.out.println("Demo logins (password 123456, admin uses admin123):");
        System.out.println("  clients : Ivan Ivanov, Maria Petrova, Sergey Sokolov");
        System.out.println("  couriers: Petr Kuryerov (Центральный), Oleg Dostavshchikov (Пригородный), Anna Kurierova (Окраинный)");
        System.out.println("  admin   : admin");

        // Graceful shutdown: stop the web server, close the database connection.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            webServer.stop();
            db.close();
        }));
    }

    /**
     * Seeds districts (with the neighbor graph), restaurants, products and demo users.
     * Idempotent: if any user already exists we assume the whole seed was applied.
     */
    private static void seedIfEmpty(UserRepository users, ClientRepository clients,
                                    CourierRepository couriers,
                                    SqliteDistrictRepository districts,
                                    RestaurantRepository restaurants,
                                    ProductRepository products) {
        if (!users.findAll().isEmpty()) {
            System.out.println("Seed data already present — skipping.");
            return;
        }

        // --- Districts + adjacency graph (chain: Центральный—Пригородный—Окраинный).
        // A courier can deliver from a restaurant in his own district or a neighboring one.
        District central = new District("Центральный");
        District suburb = new District("Пригородный");
        District outskirts = new District("Окраинный");
        districts.save(central);
        districts.save(suburb);
        districts.save(outskirts);
        // Symmetric edges are stored explicitly in both directions.
        districts.addNeighbor(central.getId(), suburb.getId());
        districts.addNeighbor(suburb.getId(), central.getId());
        districts.addNeighbor(suburb.getId(), outskirts.getId());
        districts.addNeighbor(outskirts.getId(), suburb.getId());

        // --- Restaurants: one per district, so every courier has "home" deliveries.
        Restaurant pizza = new Restaurant("Пицца «Маргарита»", "ул. Тверская, д. 10", central.getId());
        Restaurant burgers = new Restaurant("Бургерная «Гавань»", "Ленинский пр-т, д. 42", suburb.getId());
        Restaurant sushi = new Restaurant("Суши-бар «Волна»", "ул. Полевая, д. 7", outskirts.getId());
        restaurants.save(pizza);
        restaurants.save(burgers);
        restaurants.save(sushi);

        // --- Products: name + description + price + placeholder image + availability.
        products.save(new Product("Пицца Маргарита 30 см", "Томатный соус, моцарелла, свежий базилик",
                450.0, "https://placehold.co/300x200?text=Pizza+Margherita", true, pizza.getId()));
        products.save(new Product("Пицца Пепперони 30 см", "Острая пепперони, моцарелла, томатный соус",
                520.0, "https://placehold.co/300x200?text=Pizza+Pepperoni", true, pizza.getId()));
        products.save(new Product("Салат Цезарь", "Курица гриль, романо, пармезан, цезарная заправка",
                380.0, "https://placehold.co/300x200?text=Caesar+Salad", true, pizza.getId()));

        products.save(new Product("Бургер «Классик»", "Говяжья котлета, чеддер, маринованные огурцы, фирменный соус",
                390.0, "https://placehold.co/300x200?text=Classic+Burger", true, burgers.getId()));
        products.save(new Product("Картофель фри", "Хрустящий картофель с солью",
                180.0, "https://placehold.co/300x200?text=Fries", true, burgers.getId()));
        products.save(new Product("Кока-кола 0.5 л", "Охлаждённый напиток в бутылке",
                120.0, "https://placehold.co/300x200?text=Coca+Cola", true, burgers.getId()));

        products.save(new Product("Сет «Филадельфия»", "Лосось, сливочный сыр, огурец, авокадо",
                890.0, "https://placehold.co/300x200?text=Philadelphia+Set", true, sushi.getId()));
        products.save(new Product("Ролл «Калифорния»", "Краб, авокадо, огурец, икра тобико",
                450.0, "https://placehold.co/300x200?text=California+Roll", true, sushi.getId()));

        // --- Users: 1 admin, 3 clients (each with a profile), 3 couriers (one per district).
        // Passwords are demo-only; only SHA-256 hashes are stored.
        User admin = new User("admin", sha256Hex("admin123"), Role.ADMIN);
        users.save(admin);

        seedClient(users, clients, "Ivan Ivanov", "+7 900 111-22-33", "ул. Тверская, д. 1");
        seedClient(users, clients, "Maria Petrova", "+7 900 222-33-44", "Ленинский пр-т, д. 5");
        seedClient(users, clients, "Sergey Sokolov", "+7 900 333-44-55", "ул. Садовая, д. 12");

        seedCourier(users, couriers, "Petr Kuryerov", central.getId());
        seedCourier(users, couriers, "Oleg Dostavshchikov", suburb.getId());
        seedCourier(users, couriers, "Anna Kurierova", outskirts.getId());

        System.out.println("Seed data created: 3 districts (neighbor graph), 3 restaurants, " +
                "8 products, 1 admin, 3 clients, 3 couriers.");
    }

    /** Creates a client user plus its 1:1 profile row. */
    private static void seedClient(UserRepository users, ClientRepository clients,
                                   String username, String phone, String address) {
        User user = new User(username, sha256Hex("123456"), Role.CLIENT);
        users.save(user);
        clients.save(new Client(user.getId(), phone, address));
    }

    /** Creates a courier user plus its 1:1 profile row (district chosen, shift open). */
    private static void seedCourier(UserRepository users, CourierRepository couriers,
                                    String username, long districtId) {
        User user = new User(username, sha256Hex("123456"), Role.COURIER);
        users.save(user);
        Courier courier = new Courier(user.getId());
        courier.changeDistrict(districtId); // legal: not on shift yet
        courier.startShift();               // seeded "on duty" so the demo works out of the box
        couriers.save(courier);
    }

    /** SHA-256 hex digest — demonstration-level password hashing (no plain text in DB). */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }
}
