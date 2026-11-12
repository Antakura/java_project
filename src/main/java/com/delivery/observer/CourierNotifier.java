package com.delivery.observer;

/**
 * Notifies the courier about order status changes.
 */
public class CourierNotifier extends AbstractOrderNotifier {

    public CourierNotifier() {
        super("COURIER NOTIFICATION");
    }
}