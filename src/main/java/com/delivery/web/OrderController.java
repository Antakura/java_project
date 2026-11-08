package com.delivery.web;

import com.delivery.model.Client;
import com.delivery.model.Courier;
import com.delivery.model.District;
import com.delivery.model.Order;
import com.delivery.model.OrderStatus;
import com.delivery.service.OrderService;
import com.delivery.service.UserService;
import com.delivery.web.dto.CreateOrderRequest;
import com.delivery.web.dto.StatusUpdateRequest;
import io.javalin.http.Context;

import java.util.List;

/**
 * Controller for the order lifecycle: creation from cart and status transitions.
 * Status changes go through OrderService, which enforces the state machine
 * (State pattern) defined in ALLOWED_TRANSITIONS.
 */
public class OrderController {
    private final OrderService orderService;
    private final UserService userService;
    private final Courier courierActor;

    public OrderController(OrderService orderService, UserService userService, Courier courierActor) {
        this.orderService = orderService;
        this.userService = userService;
        this.courierActor = courierActor;
    }

    /** GET /api/orders */
    public void getAllOrders(Context ctx) {
        List<Order> orders = orderService.getAllOrders();
        ctx.json(orders);
    }

    /** POST /api/orders  body: CreateOrderRequest */
    public void createOrder(Context ctx) {
        CreateOrderRequest request = ctx.bodyAsClass(CreateOrderRequest.class);
        if (request == null || request.clientId() == null || request.restaurantId() == null
                || request.fromDistrict() == null || request.toDistrict() == null) {
            throw new IllegalArgumentException("Invalid order payload: clientId, restaurantId, fromDistrict and toDistrict are required.");
        }

        var userOpt = userService.getUser(request.clientId());
        if (!(userOpt.isPresent() && userOpt.get() instanceof Client client)) {
            throw new IllegalArgumentException("Client not found: " + request.clientId());
        }

        District from;
        District to;
        try {
            from = District.valueOf(request.fromDistrict().toUpperCase());
            to = District.valueOf(request.toDistrict().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown district: " + request.fromDistrict() + "/" + request.toDistrict());
        }

        Order order = orderService.createOrderFromCart(
                client, request.restaurantId(), from, to, from.distanceTo(to));
        ctx.status(201).json(order);
    }

    /** POST /api/orders/{id}/status  body: StatusUpdateRequest */
    public void updateStatus(Context ctx) {
        String orderId = ctx.pathParam("id");
        Order order = orderService.getOrder(orderId);

        StatusUpdateRequest request = ctx.bodyAsClass(StatusUpdateRequest.class);
        if (request == null || request.status() == null || request.status().isBlank()) {
            throw new IllegalArgumentException("Missing 'status' in request body.");
        }

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown status: " + request.status());
        }

        // Transitions to DELIVERING/COMPLETED must be performed by a Courier,
        // all other transitions are allowed for the order's client.
        var actor = (newStatus == OrderStatus.DELIVERING || newStatus == OrderStatus.COMPLETED)
                ? courierActor
                : order.getClient();

        orderService.updateOrderStatus(order, newStatus, actor);
        ctx.json(order);
    }
}
