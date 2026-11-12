package com.delivery.service;

import com.delivery.model.CartLine;
import com.delivery.model.Client;
import com.delivery.model.Courier;
import com.delivery.model.Order;
import com.delivery.model.OrderItem;
import com.delivery.model.OrderStatus;
import com.delivery.model.Product;
import com.delivery.model.Restaurant;
import com.delivery.model.Role;
import com.delivery.model.User;
import com.delivery.observer.OrderObserver;
import com.delivery.repository.ClientRepository;
import com.delivery.repository.CourierRepository;
import com.delivery.repository.DistrictRepository;
import com.delivery.repository.OrderRepository;
import com.delivery.repository.ProductRepository;
import com.delivery.repository.RestaurantRepository;
import com.delivery.repository.UserRepository;
import com.delivery.strategy.DeliveryCostStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Order service: creation from the cart and the whole status lifecycle.
 *
 * <h2>Patterns at work</h2>
 * <ul>
 *   <li><b>Strategy:</b> the delivery fee comes from an injected
 *       {@link DeliveryCostStrategy} — pricing policy is swappable without
 *       touching this class.</li>
 *   <li><b>State (tabular):</b> every status move goes through
 *       {@link Order#changeStatus(OrderStatus)}, which validates against the
 *       transition map on {@link OrderStatus}. This service only decides
 *       <i>who is allowed to ask</i>; the order itself decides <i>whether the
 *       move is legal</i>.</li>
 *   <li><b>Observer:</b> freshly created orders get the registered observers
 *       attached, so every later status change fans out to console/notifiers.</li>
 * </ul>
 *
 * <h2>Courier eligibility and the district graph</h2>
 * A courier may take a delivery (move it to {@code DELIVERING}) only if he is on
 * shift AND works in the restaurant's district or one of its neighbors — read from
 * the {@code district_neighbors} adjacency table. This is what the graph column in
 * the schema is for: geography as data, not hardcoded code.
 */
public class OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final RestaurantRepository restaurantRepository;
    private final DistrictRepository districtRepository;
    private final CourierRepository courierRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final DeliveryCostStrategy deliveryCostStrategy;
    /** Observers attached to every new order (Observer pattern, Subject side). */
    private final List<OrderObserver> observers;

    public OrderService(OrderRepository orderRepository,
                        ClientRepository clientRepository,
                        ProductRepository productRepository,
                        RestaurantRepository restaurantRepository,
                        DistrictRepository districtRepository,
                        CourierRepository courierRepository,
                        UserRepository userRepository,
                        CartService cartService,
                        DeliveryCostStrategy deliveryCostStrategy,
                        List<OrderObserver> observers) {
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.restaurantRepository = restaurantRepository;
        this.districtRepository = districtRepository;
        this.courierRepository = courierRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.deliveryCostStrategy = deliveryCostStrategy;
        this.observers = List.copyOf(observers);
    }

    /**
     * Checks out one restaurant's part of the cart as a new order.
     *
     * <p>Steps: resolve the client profile, re-read live prices for every line,
     * freeze them into {@code OrderItem}s, add the strategy fee, persist, and
     * clear exactly that restaurant's lines (other restaurants stay in the cart —
     * they become separate orders).</p>
     */
    public Order createOrderFromCart(long clientUserId, long restaurantId, String deliveryAddress) {
        User user = requireUser(clientUserId);
        if (user.getRole() != Role.CLIENT) {
            throw new IllegalStateException("Only clients can place orders");
        }
        Client client = clientRepository.findByIdUser(user.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Client profile not found for user " + user.getId()));
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));

        List<CartLine> lines = cartService.getLinesForRestaurant(clientUserId, restaurantId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("The cart has no items from this restaurant");
        }

        // Re-read prices now and freeze them into the order lines.
        double goodsTotal = 0.0;
        List<OrderItem> items = new ArrayList<>();
        for (CartLine line : lines) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product not found: " + line.getProductId()));
            if (!product.isAvailable()) {
                throw new IllegalStateException("Product is no longer available: " + product.getName());
            }
            items.add(OrderItem.of(0, product.getId(), line.getQuantity(), product.getPrice()));
            goodsTotal += line.getQuantity() * product.getPrice();
        }

        double fee = deliveryCostStrategy.calculateCost(goodsTotal);
        double totalPrice = Math.round((goodsTotal + fee) * 100.0) / 100.0;

        Order order = Order.create(client.getId(), restaurantId, totalPrice, deliveryAddress, items);
        for (OrderObserver observer : observers) {
            order.addObserver(observer);
        }
        orderRepository.save(order);
        cartService.clearRestaurant(clientUserId, restaurantId);
        return order;
    }

    /**
     * Moves an order to a new status if the actor is allowed to.
     *
     * <p>Authorization matrix (who may ask):</p>
     * <ul>
     *   <li>{@code COOKING}, {@code READY_FOR_DELIVERY} — ADMIN;</li>
     *   <li>{@code DELIVERING} — a COURIER on shift, in the restaurant's district
     *       or a neighboring one; the courier is bound to the order at this moment;</li>
     *   <li>{@code COMPLETED} — the COURIER assigned to this order;</li>
     *   <li>{@code CANCELLED} — the owning client or an ADMIN.</li>
     * </ul>
     * Whether the move itself is legal (e.g. no going back from DELIVERING) is
     * decided by the State pattern inside {@link Order}.
     */
    public Order updateStatus(long orderId, OrderStatus newStatus, long actorUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        User actor = requireUser(actorUserId);

        switch (newStatus) {
            case COOKING, READY_FOR_DELIVERY ->
                    requireRole(actor, Role.ADMIN, "advance the order to \"" + newStatus.labelRu() + "\"");
            case DELIVERING -> {
                Courier courier = eligibleCourierForRestaurant(actor, order.getRestaurantId());
                // Bind the courier before the transition: id_courier is filled exactly once.
                order.assignCourier(courier.getId());
            }
            case COMPLETED -> {
                Courier courier = requireCourierProfile(actor);
                if (order.getCourierId() == null || !order.getCourierId().equals(courier.getId())) {
                    throw new IllegalStateException("Only the assigned courier can complete this order");
                }
            }
            case CANCELLED -> {
                boolean isOwner = actor.getRole() == Role.CLIENT && clientRepository
                        .findByIdUser(actor.getId())
                        .map(profile -> profile.getId() == order.getClientId())
                        .orElse(false);
                if (!isOwner && actor.getRole() != Role.ADMIN) {
                    throw new IllegalStateException("Only the order owner or an admin can cancel it");
                }
            }
            case CREATED ->
                    throw new IllegalStateException("An order cannot be moved back to CREATED");
        }

        // State pattern: validates the transition, then notifies observers.
        order.changeStatus(newStatus);
        orderRepository.save(order);
        return order;
    }

    /** Order history of one client profile (newest first). */
    public List<Order> getOrderHistory(long clientProfileId) {
        return orderRepository.findByClientId(clientProfileId);
    }

    /** All orders — the admin view. */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // --- helpers --------------------------------------------------------------

    private User requireUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private void requireRole(User user, Role expected, String action) {
        if (user.getRole() != expected) {
            throw new IllegalStateException(
                    "User " + user.getUsername() + " (" + user.getRole().getDbValue()
                            + ") cannot " + action);
        }
    }

    private Courier requireCourierProfile(User actor) {
        requireRole(actor, Role.COURIER, "complete a delivery");
        return courierRepository.findByIdUser(actor.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Courier profile not found for user " + actor.getId()));
    }

    /**
     * The district-graph rule: on shift AND (courier's district == restaurant's
     * district OR a neighbor of it).
     */
    private Courier eligibleCourierForRestaurant(User actor, long restaurantId) {
        requireRole(actor, Role.COURIER, "take a delivery");
        Courier courier = courierRepository.findByIdUser(actor.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Courier profile not found for user " + actor.getId()));
        if (!courier.isOnShift()) {
            throw new IllegalStateException("The courier is not on shift");
        }
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));

        Set<Long> allowedDistricts = new HashSet<>(
                districtRepository.findNeighborIds(restaurant.getDistrictId()));
        allowedDistricts.add(restaurant.getDistrictId());

        Long courierDistrict = courier.getCurrentDistrictId();
        if (courierDistrict == null || !allowedDistricts.contains(courierDistrict)) {
            throw new IllegalStateException(
                    "The courier works in a district that is not the restaurant's and not adjacent to it");
        }
        return courier;
    }
}
