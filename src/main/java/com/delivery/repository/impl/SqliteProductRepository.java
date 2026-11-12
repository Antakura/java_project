package com.delivery.repository.impl;

import com.delivery.model.Product;
import com.delivery.repository.ProductRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * SQLite implementation of {@link ProductRepository}.
 */
public class SqliteProductRepository extends AbstractSqliteRepository<Product> implements ProductRepository {

    public SqliteProductRepository(Connection connection) {
        super(connection, "product");
    }

    @Override
    protected String table() {
        return "products";
    }

    @Override
    protected String insertColumns() {
        return "name, description, price, image_url, is_available, id_restaurant";
    }

    @Override
    protected void bindFields(PreparedStatement ps, Product product, int offset) throws SQLException {
        ps.setString(offset, product.getName());
        ps.setString(offset + 1, product.getDescription());
        ps.setDouble(offset + 2, product.getPrice());
        ps.setString(offset + 3, product.getImageUrl());
        ps.setBoolean(offset + 4, product.isAvailable());
        ps.setLong(offset + 5, product.getRestaurantId());
    }

    @Override
    protected Product readRow(ResultSet rs) throws SQLException {
        Product product = new Product(
                rs.getString("name"),
                rs.getString("description"),
                rs.getDouble("price"),
                rs.getString("image_url"),
                rs.getBoolean("is_available"),
                rs.getLong("id_restaurant"));
        product.assignId(rs.getLong("id"));
        return product;
    }

    @Override
    public List<Product> findByRestaurantId(long restaurantId) {
        return queryList("SELECT * FROM products WHERE id_restaurant = ? ORDER BY id",
                ps -> ps.setLong(1, restaurantId), this::readRow, "Error finding products by restaurant");
    }

    @Override
    public List<Product> findAvailableByRestaurantId(long restaurantId) {
        return queryList("SELECT * FROM products WHERE id_restaurant = ? AND is_available = 1 ORDER BY id",
                ps -> ps.setLong(1, restaurantId), this::readRow, "Error finding available products by restaurant");
    }
}