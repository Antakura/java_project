package com.delivery.observer;

/**
 * Logs every order status change to the console.
 */
public class ConsoleOrderObserver extends AbstractOrderNotifier {

    public ConsoleOrderObserver() {
        super("NOTIFICATION");
    }

    @Override
    protected String verb() {
        return "changed status to";
    }
}