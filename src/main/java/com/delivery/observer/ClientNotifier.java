package com.delivery.observer;

import com.delivery.model.Order;

/**
 * Notifies the client about order status changes.
 */
public class ClientNotifier implements OrderObserver {
    @Override
    public void onStatusChanged(Order order) {
        System.out.println("[CLIENT NOTIFICATION] Order " + order.getId() + " status changed to: " + order.getStatus());
    }
}
