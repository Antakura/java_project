package com.delivery.repository.impl;

import com.delivery.model.Product;
import com.delivery.repository.ProductRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link ProductRepository}.
 */
public class SqliteProductRepository implements ProductRepository {
    private final Connection connection;

    public SqliteProductRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(Product product) {
        String sql = "INSERT OR REPLACE INTO products (id, name, description, price, image_url, is_available, id_restaurant) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, product.getId());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getDescription());
            pstmt.setDouble(4, product.getPrice());
            pstmt.setString(5, product.getImageUrl());
            pstmt.setBoolean(6, product.isAvailable());
            pstmt.setLong(7, product.getRestaurantId());
            pstmt.executeUpdate();
            if (product.getId() == 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        product.assignId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving product", e);
        }
    }

    @Override
    public Optional<Product> findById(long id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readProduct(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding product", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Product> findAll() {
        String sql = "SELECT * FROM products ORDER BY id";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                products.add(readProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all products", e);
        }
        return products;
    }

    @Override
    public List<Product> findByRestaurantId(long restaurantId) {
        String sql = "SELECT * FROM products WHERE id_restaurant = ? ORDER BY id";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, restaurantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(readProduct(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding products by restaurant", e);
        }
        return products;
    }

    @Override
    public List<Product> findAvailableByRestaurantId(long restaurantId) {
        String sql = "SELECT * FROM products WHERE id_restaurant = ? AND is_available = 1 ORDER BY id";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, restaurantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(readProduct(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding available products by restaurant", e);
        }
        return products;
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting product", e);
        }
    }

    private Product readProduct(ResultSet rs) throws SQLException {
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
}
