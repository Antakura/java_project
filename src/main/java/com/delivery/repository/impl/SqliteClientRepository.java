package com.delivery.repository.impl;

import com.delivery.model.Client;
import com.delivery.repository.ClientRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link ClientRepository}.
 */
public class SqliteClientRepository implements ClientRepository {
    private final Connection connection;

    public SqliteClientRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(Client client) {
        String sql = "INSERT OR REPLACE INTO clients (id, id_user, phone, default_address) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, client.getId());
            pstmt.setLong(2, client.getIdUser());
            pstmt.setString(3, client.getPhone());
            pstmt.setString(4, client.getDefaultAddress());
            pstmt.executeUpdate();
            if (client.getId() == 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        client.assignId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving client", e);
        }
    }

    @Override
    public Optional<Client> findById(long id) {
        String sql = "SELECT * FROM clients WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readClient(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding client", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Client> findByIdUser(long userId) {
        String sql = "SELECT * FROM clients WHERE id_user = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readClient(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding client by user id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Client> findAll() {
        String sql = "SELECT * FROM clients ORDER BY id";
        List<Client> clients = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                clients.add(readClient(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all clients", e);
        }
        return clients;
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting client", e);
        }
    }

    private Client readClient(ResultSet rs) throws SQLException {
        Client client = new Client(
                rs.getLong("id_user"),
                rs.getString("phone"),
                rs.getString("default_address"));
        client.assignId(rs.getLong("id"));
        return client;
    }
}
