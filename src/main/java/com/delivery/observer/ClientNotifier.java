package com.delivery.observer;

/**
 * Notifies the client about order status changes.
 */
public class ClientNotifier extends AbstractOrderNotifier {

    public ClientNotifier() {
        super("CLIENT NOTIFICATION");
    }
}