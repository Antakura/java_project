package com.delivery.service;

import com.delivery.db.DatabaseManager;
import com.delivery.model.CartLine;
import com.delivery.model.Client;
import com.delivery.model.District;
import com.delivery.model.Product;
import com.delivery.model.Restaurant;
import com.delivery.model.Role;
import com.delivery.model.User;
import com.delivery.repository.ClientRepository;
import com.delivery.repository.ProductRepository;
import com.delivery.repository.RestaurantRepository;
import com.delivery.repository.UserRepository;
import com.delivery.repository.impl.SqliteClientRepository;
import com.delivery.repository.impl.SqliteDistrictRepository;
import com.delivery.repository.impl.SqliteProductRepository;
import com.delivery.repository.impl.SqliteRestaurantRepository;
import com.delivery.repository.impl.SqliteUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartServiceTest {

    private CartService cartService;
    private long userId;
    private long pizzaPlaceId;
    private long burgerPlaceId;
    private long pizzaId;
    private long cokeId;
    private long burgerId;

    @BeforeEach
    void setUp() {
        // Each test gets a fresh connection, i.e. its own private in-memory database.
        DatabaseManager db = new DatabaseManager("jdbc:sqlite::memory:");

        UserRepository users = new SqliteUserRepository(db.getConnection());
        ClientRepository clients = new SqliteClientRepository(db.getConnection());
        SqliteDistrictRepository districts = new SqliteDistrictRepository(db.getConnection());
        RestaurantRepository restaurants = new SqliteRestaurantRepository(db.getConnection());
        ProductRepository products = new SqliteProductRepository(db.getConnection());
        cartService = new CartService(products);

        District district = new District("Центральный");
        districts.save(district);

        Restaurant pizzaPlace = new Restaurant("Pizza Place", "Адрес 1", district.getId());
        Restaurant burgerPlace = new Restaurant("Burger House", "Адрес 2", district.getId());
        restaurants.save(pizzaPlace);
        restaurants.save(burgerPlace);
        pizzaPlaceId = pizzaPlace.getId();
        burgerPlaceId = burgerPlace.getId();

        Product pizza = new Product("Pizza", "", 12.0, "", true, pizzaPlaceId);
        Product coke = new Product("Coke", "", 2.5, "", true, pizzaPlaceId);
        Product burger = new Product("Burger", "", 10.0, "", true, burgerPlaceId);
        products.save(pizza);
        products.save(coke);
        products.save(burger);
        pizzaId = pizza.getId();
        cokeId = coke.getId();
        burgerId = burger.getId();

        User user = new User("Ivan", "hash", Role.CLIENT);
        users.save(user);
        clients.save(new Client(user.getId(), "+79991234567", "Moscow, Arbat St, 1"));
        userId = user.getId();
    }

    @Test
    void splitCartGroupsItemsByRestaurant() {
        cartService.addToCart(userId, pizzaId);
        cartService.addToCart(userId, cokeId);
        cartService.addToCart(userId, burgerId);

        Map<Long, List<CartLine>> split = cartService.getSplitByRestaurant(userId);

        assertEquals(2, split.size());
        assertEquals(2, split.get(pizzaPlaceId).size());
        assertEquals(1, split.get(burgerPlaceId).size());
    }

    @Test
    void clearRestaurantItemsRemovesOnlyThatRestaurantsItems() {
        cartService.addToCart(userId, pizzaId);
        cartService.addToCart(userId, burgerId);

        cartService.clearRestaurant(userId, pizzaPlaceId);

        assertTrue(cartService.getLinesForRestaurant(userId, pizzaPlaceId).isEmpty());
        assertEquals(1, cartService.getLinesForRestaurant(userId, burgerPlaceId).size());
    }
}