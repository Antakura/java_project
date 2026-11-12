package com.delivery.observer;

import com.delivery.model.Order;

/**
 * Shared console-notification formatting for order observers (DRY).
 *
 * <p>The three notifiers used to repeat the same {@code println} with only a
 * different tag. Subclasses supply the tag; the message wording is overridable
 * through {@link #verb()} so each notifier keeps its exact original output.</p>
 */
public abstract class AbstractOrderNotifier implements OrderObserver {

    private final String tag;

    protected AbstractOrderNotifier(String tag) {
        this.tag = tag;
    }

    @Override
    public final void onStatusChanged(Order order) {
        System.out.println("[" + tag + "] Order " + order.getId() + " " + verb() + ": " + order.getStatus());
    }

    /** Wording between the order id and the status; overridable per notifier. */
    protected String verb() {
        return "status changed to";
    }
}