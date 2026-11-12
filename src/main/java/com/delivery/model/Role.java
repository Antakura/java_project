package com.delivery.model;

/**
 * Roles a user can have in the system.
 *
 * <p>Architectural note: role is stored as a column of the {@code users} table,
 * while role-specific data lives in separate tables ({@code clients}, {@code couriers}).
 * This keeps the schema in 3NF and avoids "mixed roles in one table" anti-pattern.
 */
public enum Role {
    CLIENT("client"),
    ADMIN("admin"),
    COURIER("courier");

    /** Value stored in the database column. */
    private final String dbValue;

    Role(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    /**
     * Maps a value read from the database back to the enum.
     * @throws IllegalArgumentException if the stored value is unknown (data corruption).
     */
    public static Role fromDbValue(String value) {
        for (Role role : values()) {
            if (role.dbValue.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role in database: " + value);
    }
}
