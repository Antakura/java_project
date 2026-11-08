package com.delivery.repository.impl;

import com.delivery.model.User;
import com.delivery.repository.UserRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of UserRepository.
 */
public class SqliteUserRepository implements UserRepository {
    private final Connection connection;

    public SqliteUserRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(User user) {
        String sql = "INSERT OR REPLACE INTO users (id, name, phone_number) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getPhoneNumber());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving user", e);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Note: Since User is abstract, we'd ideally need a factory or 
                    // know the specific type. For simplicity, we'll return a dummy 
                    // or assume we're dealing with a specific type in a real app.
                    // Here, we'll just return a generic User-like object if we had one,
                    // but since User is abstract, let's assume we return a Client for demo.
                    // In a real system, we'd store the type in the DB.
                    return Optional.of(new com.delivery.model.Client(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("phone_number"),
                            "Unknown Address"
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user", e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new com.delivery.model.Client(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("phone_number"),
                        "Unknown Address"
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all users", e);
        }
        return users;
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting user", e);
        }
    }
}
