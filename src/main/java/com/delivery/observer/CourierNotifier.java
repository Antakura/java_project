package com.delivery.observer;

import com.delivery.model.Order;

/**
 * Notifies the courier about order status changes.
 */
public class CourierNotifier implements OrderObserver {
    @Override
    public void onStatusChanged(Order order) {
        System.out.println("[COURIER NOTIFICATION] Order " + order.getId() + " status changed to: " + order.getStatus());
    }
}
