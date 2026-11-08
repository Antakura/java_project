package com.delivery.service;

import com.delivery.model.Client;
import com.delivery.model.Courier;
import com.delivery.model.District;
import com.delivery.model.Order;
import com.delivery.model.OrderStatus;
import com.delivery.model.Product;
import com.delivery.repository.InMemoryOrderRepository;
import com.delivery.strategy.FlatRateDeliveryStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceTest {

    private Client client;
    private Courier courier;
    private CartService cartService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        client = new Client("C1", "Ivan", "+79991234567", "Moscow, Arbat St, 1");
        courier = new Courier("CO1", "Alex", "+79997654321", "Bicycle");
        cartService = new CartService();
        orderService = new OrderService(new InMemoryOrderRepository(), new FlatRateDeliveryStrategy(5.0), cartService);
    }

    @Test
    void fullLifecycleTransitionsSucceed() {
        Order order = createOrderWithItems("R1");

        orderService.updateOrderStatus(order, OrderStatus.COOKING, client);
        orderService.updateOrderStatus(order, OrderStatus.READY_FOR_DELIVERY, client);
        orderService.updateOrderStatus(order, OrderStatus.DELIVERING, courier);
        orderService.updateOrderStatus(order, OrderStatus.COMPLETED, courier);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void skippingStepsIsRejected() {
        Order order = createOrderWithItems("R1");

        // Jumping straight to COMPLETED (or DELIVERING) must be rejected by the state machine.
        assertThrows(IllegalStateException.class, () ->
                orderService.updateOrderStatus(order, OrderStatus.COMPLETED, courier));
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void terminalStatesCannotBeChanged() {
        Order order = createOrderWithItems("R1");
        orderService.updateOrderStatus(order, OrderStatus.CANCELLED, client);

        assertThrows(IllegalStateException.class, () ->
                orderService.updateOrderStatus(order, OrderStatus.COMPLETED, courier));
    }

    @Test
    void onlyCourierCanStartDeliveryAndComplete() {
        Order order = createOrderWithItems("R1");
        orderService.updateOrderStatus(order, OrderStatus.COOKING, client);
        orderService.updateOrderStatus(order, OrderStatus.READY_FOR_DELIVERY, client);

        assertThrows(IllegalStateException.class, () ->
                orderService.updateOrderStatus(order, OrderStatus.DELIVERING, client));
    }

    @Test
    void cartIsSplitByRestaurantAndOnlyOrderedItemsAreCleared() {
        cartService.addToCart(client, new Product("P1", "Margherita Pizza", 12.0, "R1"));
        cartService.addToCart(client, new Product("P3", "Big Burger", 10.0, "R2"));

        Order orderA = orderService.createOrderFromCart(client, "R1", District.CENTRAL, District.CENTRAL, 5.0);
        assertEquals(1, orderA.getProducts().size());

        // Regression test: the first checkout must not wipe out items of other restaurants.
        Order orderB = orderService.createOrderFromCart(client, "R2", District.OUTSKIRTS, District.CENTRAL, 10.0);
        assertEquals(1, orderB.getProducts().size());

        // The cart is now empty for both restaurants.
        assertThrows(IllegalStateException.class, () ->
                orderService.createOrderFromCart(client, "R1", District.CENTRAL, District.CENTRAL, 5.0));
    }

    private Order createOrderWithItems(String restaurantId) {
        cartService.addToCart(client, new Product("P1", "Margherita Pizza", 12.0, restaurantId));
        return orderService.createOrderFromCart(client, restaurantId, District.CENTRAL, District.CENTRAL, 5.0);
    }
}
