package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.OrderRepository;
import com.delivery.strategy.DeliveryCostStrategy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing the lifecycle of orders.
 */
public class OrderService {
    /**
     * Explicit state machine: allowed transitions per current status.
     * Terminal states (COMPLETED, CANCELLED) have no outgoing transitions.
     * An order can be cancelled until the courier picks it up; once delivery
     * has started, only completion is possible.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.COOKING, OrderStatus.CANCELLED),
            OrderStatus.COOKING, Set.of(OrderStatus.READY_FOR_DELIVERY, OrderStatus.CANCELLED),
            OrderStatus.READY_FOR_DELIVERY, Set.of(OrderStatus.DELIVERING, OrderStatus.CANCELLED),
            OrderStatus.DELIVERING, Set.of(OrderStatus.COMPLETED)
    );

    private final OrderRepository orderRepository;
    private DeliveryCostStrategy deliveryCostStrategy;
    private final CartService cartService;

    public OrderService(OrderRepository orderRepository, DeliveryCostStrategy deliveryCostStrategy, CartService cartService) {
        this.orderRepository = orderRepository;
        this.deliveryCostStrategy = deliveryCostStrategy;
        this.cartService = cartService;
    }

    public void setDeliveryCostStrategy(DeliveryCostStrategy strategy) {
        this.deliveryCostStrategy = strategy;
    }

    /**
     * Creates an order from a specific restaurant's items in the client's cart.
     */
    public Order createOrderFromCart(Client client, String restaurantId, District from, District to, double distance) {
        List<Product> products = cartService.getItemsForRestaurant(client.getId(), restaurantId);

        if (products.isEmpty()) {
            throw new IllegalStateException("Cart is empty for restaurant: " + restaurantId);
        }

        Order order = new Order("ORD-" + UUID.randomUUID(), client, restaurantId, from, to, distance);
        for (Product p : products) {
            order.addProduct(p);
        }

        orderRepository.save(order);
        cartService.clearRestaurantItems(client.getId(), restaurantId);
        return order;
    }

    public void updateOrderStatus(Order order, OrderStatus newStatus, User actor) {
        validateTransition(order.getStatus(), newStatus, actor);
        order.updateStatus(newStatus);
        orderRepository.save(order);
    }

    private void validateTransition(OrderStatus current, OrderStatus next, User actor) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw new IllegalStateException("Illegal status transition: " + current + " -> " + next);
        }

        switch (next) {
            case DELIVERING:
            case COMPLETED:
                if (!(actor instanceof Courier)) {
                    throw new IllegalStateException("Only a Courier can set order to " + next + ".");
                }
                break;
            default:
                break;
        }
    }

    public double calculateTotalAmount(Order order) {
        return order.getTotalPrice() + deliveryCostStrategy.calculateCost(order.getFromDistrict(), order.getToDistrict());
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
