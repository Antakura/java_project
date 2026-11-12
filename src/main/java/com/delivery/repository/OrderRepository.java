package com.delivery.repository;

import com.delivery.model.Order;

import java.util.List;

/**
 * Repository for {@code orders} together with their {@code order_items}.
 */
public interface OrderRepository extends Repository<Order> {

    /** Order history of one client (newest first). */
    List<Order> findByClientId(long clientId);
}
