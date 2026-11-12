package com.delivery.repository.impl;

import com.delivery.model.Order;
import com.delivery.model.OrderItem;
import com.delivery.model.OrderStatus;
import com.delivery.repository.OrderRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link OrderRepository}.
 *
 * <h2>Why this class is the heart of the bug fix</h2>
 * The old version rebuilt a persisted order by calling the business method
 * {@code addProduct(...)} for every line and forcing the stored status through
 * reflection. Because {@code addProduct} refuses to work on any order that is not
 * {@code CREATED}, reading back an order that had already moved to {@code COOKING}
 * threw "Cannot add products to an order that is already COOKING" — which broke the
 * whole "Заказы" tab after checkout.
 *
 * <p>Now every read goes through {@link Order#rehydrate(...)} and
 * {@link OrderItem#rehydrate(...)}: persistence restores stored state verbatim and
 * never re-runs business rules. No reflection, no business methods on the read path.</p>
 *
 * <h2>Transactional save</h2>
 * An order row and its item rows are one logical unit: they are written inside a
 * single transaction so a crash can never leave an order without lines (or orphan
 * lines). On update the old lines are deleted and re-inserted — simple, correct,
 * and cheap at this scale.
 */
public class SqliteOrderRepository implements OrderRepository {
    private final Connection connection;

    public SqliteOrderRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(Order order) {
        try {
            connection.setAutoCommit(false);

            long orderId = order.getId();
            if (orderId == 0) {
                // New order: let SQLite generate the primary key.
                String sql = "INSERT INTO orders (id_client, id_restaurant, id_courier, status, " +
                        "total_price, delivery_address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    bindOrder(pstmt, order);
                    pstmt.executeUpdate();
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No generated key for a new order");
                        }
                        orderId = keys.getLong(1);
                        order.assignId(orderId);
                    }
                }
            } else {
                // Existing order: replace the row (items are replaced below).
                String sql = "INSERT OR REPLACE INTO orders (id, id_client, id_restaurant, id_courier, " +
                        "status, total_price, delivery_address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setLong(1, orderId);
                    bindOrder(pstmt, order);
                    pstmt.executeUpdate();
                }
            }

            // Replace the item rows: delete old lines, insert current ones.
            // The FK is written from the parent order's id — for a brand-new order
            // the items were built before it had an id, so we must not trust
            // item.getOrderId() here.
            try (PreparedStatement del = connection.prepareStatement(
                    "DELETE FROM order_items WHERE id_order = ?")) {
                del.setLong(1, orderId);
                del.executeUpdate();
            }
            String itemSql = "INSERT INTO order_items (id_order, id_product, quantity, price_at_order) " +
                    "VALUES (?, ?, ?, ?)";
            for (OrderItem item : order.getItems()) {
                try (PreparedStatement pstmt = connection.prepareStatement(itemSql)) {
                    pstmt.setLong(1, orderId);
                    pstmt.setLong(2, item.getProductId());
                    pstmt.setInt(3, item.getQuantity());
                    pstmt.setDouble(4, item.getPriceAtOrder());
                    pstmt.executeUpdate();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            rollback(e);
            throw new RuntimeException("Error saving order", e);
        } finally {
            restoreAutoCommit();
        }
    }

    @Override
    public Optional<Order> findById(long id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding order", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Order> findAll() {
        String sql = "SELECT * FROM orders ORDER BY id";
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                orders.add(readOrder(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all orders", e);
        }
        return orders;
    }

    @Override
    public List<Order> findByClientId(long clientId) {
        String sql = "SELECT * FROM orders WHERE id_client = ? ORDER BY created_at DESC, id DESC";
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, clientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(readOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding orders by client", e);
        }
        return orders;
    }

    @Override
    public void delete(long id) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement delItems = connection.prepareStatement(
                    "DELETE FROM order_items WHERE id_order = ?")) {
                delItems.setLong(1, id);
                delItems.executeUpdate();
            }
            try (PreparedStatement delOrder = connection.prepareStatement(
                    "DELETE FROM orders WHERE id = ?")) {
                delOrder.setLong(1, id);
                delOrder.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            rollback(e);
            throw new RuntimeException("Error deleting order", e);
        } finally {
            restoreAutoCommit();
        }
    }

    /** Binds the shared columns of an order row (used by both insert paths). */
    private void bindOrder(PreparedStatement pstmt, Order order) throws SQLException {
        pstmt.setLong(1, order.getClientId());
        pstmt.setLong(2, order.getRestaurantId());
        if (order.getCourierId() == null) {
            pstmt.setNull(3, java.sql.Types.INTEGER);
        } else {
            pstmt.setLong(3, order.getCourierId());
        }
        pstmt.setString(4, order.getStatus().name());
        pstmt.setDouble(5, order.getTotalPrice());
        pstmt.setString(6, order.getDeliveryAddress());
        // Instant is stored as ISO-8601 TEXT — sortable and lossless.
        pstmt.setString(7, order.getCreatedAt().toString());
    }

    /**
     * Rehydrates an order row <b>and its item rows</b>. This is the read path that
     * used to crash: it now calls {@link Order#rehydrate(...)} and never touches
     * business methods.
     */
    private Order readOrder(ResultSet rs) throws SQLException {
        long orderId = rs.getLong("id");

        List<OrderItem> items = new ArrayList<>();
        String itemSql = "SELECT * FROM order_items WHERE id_order = ? ORDER BY id";
        try (PreparedStatement pstmt = connection.prepareStatement(itemSql)) {
            pstmt.setLong(1, orderId);
            try (ResultSet itemRs = pstmt.executeQuery()) {
                while (itemRs.next()) {
                    items.add(OrderItem.rehydrate(
                            itemRs.getLong("id"),
                            orderId,
                            itemRs.getLong("id_product"),
                            itemRs.getInt("quantity"),
                            itemRs.getDouble("price_at_order")));
                }
            }
        }

        long courierId = rs.getLong("id_courier");
        Long nullableCourier = rs.wasNull() ? null : courierId;

        return Order.rehydrate(
                orderId,
                rs.getLong("id_client"),
                rs.getLong("id_restaurant"),
                nullableCourier,
                OrderStatus.valueOf(rs.getString("status")),
                rs.getDouble("total_price"),
                rs.getString("delivery_address"),
                Instant.parse(rs.getString("created_at")),
                items);
    }

    private void rollback(SQLException cause) {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            throw new RuntimeException("Error rolling back transaction", ex);
        }
    }

    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Error resetting auto-commit", e);
        }
    }
}
