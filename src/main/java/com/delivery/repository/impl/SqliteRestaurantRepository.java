package com.delivery.repository.impl;

import com.delivery.model.Restaurant;
import com.delivery.repository.RestaurantRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link RestaurantRepository}.
 */
public class SqliteRestaurantRepository implements RestaurantRepository {
    private final Connection connection;

    public SqliteRestaurantRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(Restaurant restaurant) {
        String sql = "INSERT OR REPLACE INTO restaurants (id, name, address, id_district) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, restaurant.getId());
            pstmt.setString(2, restaurant.getName());
            pstmt.setString(3, restaurant.getAddress());
            pstmt.setLong(4, restaurant.getDistrictId());
            pstmt.executeUpdate();
            if (restaurant.getId() == 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        restaurant.assignId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving restaurant", e);
        }
    }

    @Override
    public Optional<Restaurant> findById(long id) {
        String sql = "SELECT * FROM restaurants WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readRestaurant(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding restaurant", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Restaurant> findAll() {
        String sql = "SELECT * FROM restaurants ORDER BY id";
        List<Restaurant> restaurants = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                restaurants.add(readRestaurant(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all restaurants", e);
        }
        return restaurants;
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM restaurants WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting restaurant", e);
        }
    }

    private Restaurant readRestaurant(ResultSet rs) throws SQLException {
        Restaurant restaurant = new Restaurant(
                rs.getString("name"),
                rs.getString("address"),
                rs.getLong("id_district"));
        restaurant.assignId(rs.getLong("id"));
        return restaurant;
    }
}
