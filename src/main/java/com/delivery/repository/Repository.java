package com.delivery.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository contract (Layered Architecture: the persistence layer is
 * hidden behind this interface, so services never touch JDBC directly).
 *
 * <p>All entities use database-generated {@code long} primary keys.</p>
 *
 * @param <T> the entity type
 */
public interface Repository<T> {

    /**
     * Inserts a new entity or updates an existing one (matched by id).
     * For a new entity the generated primary key is written back via
     * {@code assignId(...)} on the same instance.
     */
    void save(T entity);

    Optional<T> findById(long id);

    List<T> findAll();

    void delete(long id);
}
