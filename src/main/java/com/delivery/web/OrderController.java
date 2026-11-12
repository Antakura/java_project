package com.delivery.web;

import com.delivery.model.Client;
import com.delivery.model.Order;
import com.delivery.model.OrderItem;
import com.delivery.model.OrderStatus;
import com.delivery.model.Product;
import com.delivery.model.Restaurant;
import com.delivery.model.User;
import com.delivery.repository.ClientRepository;
import com.delivery.repository.ProductRepository;
import com.delivery.repository.RestaurantRepository;
import com.delivery.repository.UserRepository;
import com.delivery.service.OrderService;
import com.delivery.web.dto.CreateOrderRequest;
import com.delivery.web.dto.OrderDto;
import com.delivery.web.dto.OrderItemDto;
import com.delivery.web.dto.StatusUpdateRequest;
import io.javalin.http.Context;

import java.util.List;

/**
 * Order endpoints: checkout, history and status transitions.
 *
 * <p>This is the endpoint family that used to crash with
 * "Cannot add products to an order that is already COOKING": every read now goes
 * through {@code Order.rehydrate(...)} in the repository, so listing orders works
 * at any lifecycle stage.</p>
 */
public class OrderController {

    private final OrderService orderService;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final ProductRepository productRepository;

    public OrderController(OrderService orderService,
                           ClientRepository clientRepository,
                           UserRepository userRepository,
                           RestaurantRepository restaurantRepository,
                           ProductRepository productRepository) {
        this.orderService = orderService;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.productRepository = productRepository;
    }

    /** GET /api/orders — all orders (admin view). */
    public void getAllOrders(Context ctx) {
        List<OrderDto> orders = orderService.getAllOrders().stream()
                .map(this::toDto)
                .toList();
        ctx.json(orders);
    }

    /** GET /api/orders/user/{userId} — the client's own history. */
    public void getClientHistory(Context ctx) {
        long userId = Long.parseLong(ctx.pathParam("userId"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Client client = clientRepository.findByIdUser(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User " + userId + " has no client profile"));
        List<OrderDto> orders = orderService.getOrderHistory(client.getId()).stream()
                .map(this::toDto)
                .toList();
        ctx.json(orders);
    }

    /** POST /api/orders — checkout one restaurant's part of the cart. */
    public void createOrder(Context ctx) {
        CreateOrderRequest request = ctx.bodyAsClass(CreateOrderRequest.class);
        Order order = orderService.createOrderFromCart(
                request.userId(), request.restaurantId(), request.deliveryAddress());
        ctx.status(201).json(toDto(order));
    }

    /** POST /api/orders/{id}/status — role-checked status transition. */
    public void updateStatus(Context ctx) {
        long orderId = Long.parseLong(ctx.pathParam("id"));
        StatusUpdateRequest request = ctx.bodyAsClass(StatusUpdateRequest.class);
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown status: " + request.status());
        }
        Order order = orderService.updateStatus(orderId, newStatus, request.actorId());
        ctx.json(toDto(order));
    }

    /** Domain -> DTO with denormalized names (one lookup per reference). */
    private OrderDto toDto(Order order) {
        String clientName = clientRepository.findById(order.getClientId())
                .flatMap(c -> userRepository.findById(c.getIdUser()))
                .map(User::getUsername)
                .orElse("?");
        String restaurantName = restaurantRepository.findById(order.getRestaurantId())
                .map(Restaurant::getName)
                .orElse("?");

        List<OrderItemDto> items = order.getItems().stream()
                .map(this::toItemDto)
                .toList();

        return new OrderDto(
                order.getId(),
                order.getClientId(), clientName,
                order.getRestaurantId(), restaurantName,
                order.getCourierId(),
                order.getStatus().name(), order.getStatus().labelRu(),
                order.getTotalPrice(),
                order.getDeliveryAddress(),
                order.getCreatedAt().toString(),
                items);
    }

    private OrderItemDto toItemDto(OrderItem item) {
        String name = productRepository.findById(item.getProductId())
                .map(Product::getName)
                .orElse("?");
        return new OrderItemDto(item.getProductId(), name,
                item.getQuantity(), item.getPriceAtOrder());
    }
}
