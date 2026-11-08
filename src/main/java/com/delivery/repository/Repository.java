package com.delivery.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface.
 * @param <T> The type of the entity
 */
public interface Repository<T> {
    /**
     * Saves an entity in the storage.
     * @param entity The entity to save
     */
    void save(T entity);

    /**
     * Finds an entity by its identifier.
     * @param id The identifier of the entity
     * @return Optional containing the entity if it is found
     */
    Optional<T> findById(String id);

    /**
     * Returns a list of all entities.
     * @return A list of all stored entities
     */
    List<T> findAll();

    /**
     * Removes an entity from the storage by its identifier.
     * @param id The identifier of the entity to remove
     */
    void delete(String id);
}
