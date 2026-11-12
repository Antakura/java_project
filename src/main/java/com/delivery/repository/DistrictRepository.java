package com.delivery.repository;

import com.delivery.model.District;

import java.util.Set;

/**
 * Repository for {@code districts} and their adjacency graph.
 */
public interface DistrictRepository extends Repository<District> {

    /**
     * Reads the neighbor graph: ids of districts directly connected to the given one.
     * Used by {@code OrderService} to decide which couriers may take an order
     * (same district or a neighboring one).
     */
    Set<Long> findNeighborIds(long districtId);
}
