package com.delivery.model;

/**
 * A city district.
 *
 * <p><b>Why an entity instead of the old {@code District} enum?</b> The target schema
 * stores districts in a table plus a {@code district_neighbors} adjacency graph, so
 * the set of districts is data, not code. The hardcoded distance matrix that lived on
 * the old enum was removed together with it: geographic pricing no longer exists
 * (see the reworked Strategy package), while the neighbor graph now drives courier
 * eligibility in {@code OrderService}.</p>
 */
public class District extends AbstractEntity {
    private final String name;

    public District(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("District name must not be blank");
        }
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "District[id=" + id + ", name=" + name + "]";
    }
}
