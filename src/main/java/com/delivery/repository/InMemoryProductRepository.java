package com.delivery.repository;

import com.delivery.model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of the product repository.
 * Uses ConcurrentHashMap to ensure thread safety for read/write operations.
 */
public class InMemoryProductRepository implements ProductRepository {
    private final Map<String, Product> products = new ConcurrentHashMap<>();

    @Override
    public void save(Product product) {
        products.put(product.getId(), product);
    }

    @Override
    public Optional<Product> findById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    @Override
    public void delete(String id) {
        products.remove(id);
    }
}
