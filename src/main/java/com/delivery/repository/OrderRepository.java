package com.delivery.repository;

import com.delivery.model.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends Repository<Order> {
    Optional<Order> findById(String id);
}
