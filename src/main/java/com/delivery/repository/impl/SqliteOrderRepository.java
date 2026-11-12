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
public class SqliteOrderRepository extends AbstractSqliteRepository<Order> implements OrderRepository {

    public SqliteOrderRepository(Connection connection) {
        super(connection, "order");
    }

    @Override
    protected String table() {
        return "orders";
    }

    @Override
    protected String insertColumns() {
        return "id_client, id_restaurant, id_courier, status, total_price, delivery_address, created_at";
    }

    @Override
    protected void bindFields(PreparedStatement ps, Order order, int offset) throws SQLException {
        ps.setLong(offset, order.getClientId());
        ps.setLong(offset + 1, order.getRestaurantId());
        if (order.getCourierId() == null) {
            ps.setNull(offset + 2, java.sql.Types.INTEGER);
        } else {
            ps.setLong(offset + 2, order.getCourierId());
        }
        ps.setString(offset + 3, order.getStatus().name());
        ps.setDouble(offset + 4, order.getTotalPrice());
        ps.setString(offset + 5, order.getDeliveryAddress());
        // Instant is stored as ISO-8601 TEXT — sortable and lossless.
        ps.setString(offset + 6, order.getCreatedAt().toString());
    }

    @Override
    public void save(Order order) {
        inTransaction("Error saving order", () -> {
            long orderId = order.getId();
            if (orderId == 0) {
                // New order: let SQLite generate the primary key.
                String sql = "INSERT INTO orders (id_client, id_restaurant, id_courier, status, "
                        + "total_price, delivery_address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
                orderId = executeInsert(sql, ps -> bindFields(ps, order, 1));
                order.assignId(orderId);
            } else {
                // Existing order: replace the row (items are replaced below).
                String sql = "INSERT OR REPLACE INTO orders (id, id_client, id_restaurant, id_courier, "
                        + "status, total_price, delivery_address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                long existingId = orderId;
                executeUpdate(sql, ps -> {
                    ps.setLong(1, existingId);
                    bindFields(ps, order, 2);
                });
            }

            // Replace the item rows: delete old lines, insert current ones.
            // The FK is written from the parent order's id — for a brand-new order
            // the items were built before it had an id, so we must not trust
            // item.getOrderId() here.
            long parentId = orderId;
            executeUpdate("DELETE FROM order_items WHERE id_order = ?", ps -> ps.setLong(1, parentId));
            String itemSql = "INSERT INTO order_items (id_order, id_product, quantity, price_at_order) "
                    + "VALUES (?, ?, ?, ?)";
            for (OrderItem item : order.getItems()) {
                executeUpdate(itemSql, ps -> {
                    ps.setLong(1, parentId);
                    ps.setLong(2, item.getProductId());
                    ps.setInt(3, item.getQuantity());
                    ps.setDouble(4, item.getPriceAtOrder());
                });
            }
        });
    }

    @Override
    public List<Order> findByClientId(long clientId) {
        return queryList("SELECT * FROM orders WHERE id_client = ? ORDER BY created_at DESC, id DESC",
                ps -> ps.setLong(1, clientId), this::readRow, "Error finding orders by client");
    }

    @Override
    public void delete(long id) {
        // Delete the child lines first, then the order — one atomic unit.
        inTransaction("Error deleting order", () -> {
            executeUpdate("DELETE FROM order_items WHERE id_order = ?", ps -> ps.setLong(1, id));
            executeUpdate("DELETE FROM orders WHERE id = ?", ps -> ps.setLong(1, id));
        });
    }

    /**
     * Rehydrates an order row <b>and its item rows</b>. This is the read path that
     * used to crash: it now calls {@link Order#rehydrate(...)} and never touches
     * business methods.
     */
    @Override
    protected Order readRow(ResultSet rs) throws SQLException {
        long orderId = rs.getLong("id");

        List<OrderItem> items = new ArrayList<>();
        String itemSql = "SELECT * FROM order_items WHERE id_order = ? ORDER BY id";
        try (PreparedStatement ps = connection.prepareStatement(itemSql)) {
            ps.setLong(1, orderId);
            try (ResultSet itemRs = ps.executeQuery()) {
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

        return Order.rehydrate(
                orderId,
                rs.getLong("id_client"),
                rs.getLong("id_restaurant"),
                nullableLong(rs, "id_courier"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getDouble("total_price"),
                rs.getString("delivery_address"),
                Instant.parse(rs.getString("created_at")),
                items);
    }
}