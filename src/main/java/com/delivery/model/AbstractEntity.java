package com.delivery.model;

/**
 * Base class for persisted entities with a database-generated {@code long} primary key.
 *
 * <p>Centralizes the id lifecycle shared by every entity (DRY): the id is 0 until
 * the storage layer records the SQLite AUTOINCREMENT value through {@link #assignId(long)},
 * which may happen exactly once. The field is {@code protected} so rehydrating
 * constructors (e.g. {@code Order}, {@code Courier.rehydrate}) can restore a stored
 * id directly without going through the assign-once guard.</p>
 */
public abstract class AbstractEntity {
    /** Assigned by the database on insert; 0 until persisted. */
    protected long id;

    public long getId() {
        return id;
    }

    /**
     * Storage-layer hook: records the generated primary key after INSERT.
     * An id can be assigned exactly once, keeping the entity immutable for all
     * practical purposes.
     */
    public void assignId(long id) {
        if (this.id != 0) {
            throw new IllegalStateException(getClass().getSimpleName() + " already has an id: " + this.id);
        }
        this.id = id;
    }
}