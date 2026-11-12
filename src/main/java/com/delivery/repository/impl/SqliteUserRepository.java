package com.delivery.repository.impl;

import com.delivery.model.Role;
import com.delivery.model.User;
import com.delivery.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link UserRepository}.
 */
public class SqliteUserRepository implements UserRepository {
    private final Connection connection;

    public SqliteUserRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(User user) {
        String sql = "INSERT OR REPLACE INTO users (id, username, password_hash, role) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            setParams(pstmt, user);
            pstmt.executeUpdate();
            if (user.getId() == 0) {
                // New entity: record the AUTOINCREMENT value on the instance.
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.assignId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving user", e);
        }
    }

    private void setParams(PreparedStatement pstmt, User user) throws SQLException {
        pstmt.setLong(1, user.getId());
        pstmt.setString(2, user.getUsername());
        pstmt.setString(3, user.getPasswordHash());
        pstmt.setString(4, user.getRole().getDbValue());
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by username", e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> users = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(readUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all users", e);
        }
        return users;
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting user", e);
        }
    }

    private User readUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getString("username"),
                rs.getString("password_hash"),
                Role.fromDbValue(rs.getString("role")));
        user.assignId(rs.getLong("id"));
        return user;
    }
}
