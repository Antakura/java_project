package com.delivery.observer;

import com.delivery.model.Order;

public class ConsoleOrderObserver implements OrderObserver {
    @Override
    public void onStatusChanged(Order order) {
        System.out.println("[NOTIFICATION] Order " + order.getId() + " changed status to: " + order.getStatus());
    }
}
