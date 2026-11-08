package com.delivery;

import com.delivery.db.DatabaseManager;
import com.delivery.model.Client;
import com.delivery.model.Courier;
import com.delivery.model.District;
import com.delivery.model.Product;
import com.delivery.model.Restaurant;
import com.delivery.repository.InMemoryOrderRepository;
import com.delivery.repository.InMemoryProductRepository;
import com.delivery.repository.InMemoryRestaurantRepository;
import com.delivery.service.CartService;
import com.delivery.service.OrderService;
import com.delivery.service.UserService;
import com.delivery.strategy.StandardDistrictStrategy;
import com.delivery.web.WebServer;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting NyamNyam System...");

        // 1. Initialize Database (SQLite)
        DatabaseManager dbManager = new DatabaseManager();

        // 2. Initialize Repositories: users are persisted in SQLite,
        //    the rest of the domain data lives in memory for this demo.
        var userRepository = new com.delivery.repository.impl.SqliteUserRepository(dbManager.getConnection());
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();

        // 3. Initialize Services (dependency injection, no static state)
        UserService userService = new UserService(userRepository);
        CartService cartService = new CartService();
        OrderService orderService = new OrderService(
                orderRepository, new StandardDistrictStrategy(99.0), cartService);

        // 4. Seed demo data
        seedUsers(userService);
        seedMenu(productRepository, restaurantRepository);

        // The courier acts as the actor for DELIVERING/COMPLETED transitions.
        Courier courierActor = new Courier("COURIER-1", "Petr Kuryerov", "+79990001122", "scooter");

        // 5. Start Web Server (frontend + REST API)
        WebServer webServer = new WebServer(userService, productRepository, restaurantRepository,
                cartService, orderService, courierActor);

        System.out.println("System is running. Frontend: http://localhost:7000/");

        // Add a shutdown hook to close the database connection
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            webServer.stop();
            dbManager.close();
        }));
    }

    private static void seedUsers(UserService userService) {
        // Check if user exists, if not, create one (persisted in SQLite).
        if (userService.getUser("1").isEmpty()) {
            userService.registerUser(new Client("1", "Ivan Ivanov", "+79991234567", "Moscow, Arbat"));
            System.out.println("Seed data: Created test user 'Ivan Ivanov'");
        } else {
            System.out.println("Seed data: Test user already exists.");
        }
    }

    private static void seedMenu(InMemoryProductRepository products, InMemoryRestaurantRepository restaurants) {
        Restaurant pizza = new Restaurant("R1", "Пицца «Маргарита»", District.CENTRAL);
        Restaurant burgers = new Restaurant("R2", "Бургерная «Гавань»", District.SUBURB);
        Restaurant sushi = new Restaurant("R3", "Суши-бар «Волна»", District.OUTSKIRTS);

        restaurants.save(pizza);
        restaurants.save(burgers);
        restaurants.save(sushi);

        products.save(new Product("P1", "Пицца Маргарита 30 см", 450.0, pizza.getId()));
        products.save(new Product("P2", "Пицца Пепперони 30 см", 520.0, pizza.getId()));
        products.save(new Product("P3", "Салат Цезарь", 380.0, pizza.getId()));

        products.save(new Product("P4", "Бургер «Классик»", 390.0, burgers.getId()));
        products.save(new Product("P5", "Картофель фри", 180.0, burgers.getId()));
        products.save(new Product("P6", "Кока-кола 0.5 л", 120.0, burgers.getId()));

        products.save(new Product("P7", "Сет «Филадельфия»", 890.0, sushi.getId()));
        products.save(new Product("P8", "Ролл «Калифорния»", 450.0, sushi.getId()));

        System.out.println("Seed data: Menu loaded (3 restaurants, 8 products).");
    }
}
