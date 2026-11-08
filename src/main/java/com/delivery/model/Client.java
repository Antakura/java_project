package com.delivery.model;

/**
 * Represents a customer who places orders.
 */
public class Client extends User {
    private final String address;

    public Client(String id, String name, String phoneNumber, String address) {
        super(id, name, phoneNumber);
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return super.toString() + " [address=" + address + "]";
    }
}
