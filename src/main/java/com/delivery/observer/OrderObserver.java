package com.delivery.observer;

import com.delivery.model.Order;

/**
 * Interface for observers that want to listen to Order status changes.
 */
public interface OrderObserver {
    void onStatusChanged(Order order);
}
