package com.delivery.repository;

import com.delivery.model.Product;
import java.util.Optional;

/**
 * Repository interface for managing products.
 */
public interface ProductRepository extends Repository<Product> {
    Optional<Product> findById(String id);
}
