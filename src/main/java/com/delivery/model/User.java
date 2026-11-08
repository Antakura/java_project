package com.delivery.model;

/**
 * Abstract base class for all users in the system.
 * Demonstrates encapsulation by keeping fields private and providing getters.
 */
public abstract class User {
    private final String id;
    private final String name;
    private final String phoneNumber;

    protected User(String id, String name, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, name=%s]", this.getClass().getSimpleName(), id, name);
    }
}
