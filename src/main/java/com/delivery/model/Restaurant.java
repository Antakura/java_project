package com.delivery.model;

/**
 * A restaurant that sells products.
 *
 * <p>The district is now a foreign key ({@code id_district}) to the {@code districts}
 * table instead of an enum constant, matching the target schema. Restaurants are
 * immutable: per the specification only products are edited by the admin.</p>
 */
public class Restaurant extends AbstractEntity {
    private final String name;
    private final String address;
    /** FK -> districts.id. */
    private final long districtId;

    public Restaurant(String name, String address, long districtId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Restaurant name must not be blank");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Restaurant address must not be blank");
        }
        if (districtId <= 0) {
            throw new IllegalArgumentException("Restaurant must belong to a district");
        }
        this.name = name.trim();
        this.address = address.trim();
        this.districtId = districtId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public long getDistrictId() {
        return districtId;
    }

    @Override
    public String toString() {
        return "Restaurant[id=" + id + ", name=" + name + ", district=" + districtId + "]";
    }
}
