package com.delivery.repository.impl;

import com.delivery.model.District;
import com.delivery.repository.DistrictRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * SQLite implementation of {@link DistrictRepository}, including the
 * {@code district_neighbors} adjacency graph.
 */
public class SqliteDistrictRepository implements DistrictRepository {
    private final Connection connection;

    public SqliteDistrictRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(District district) {
        String sql = "INSERT OR REPLACE INTO districts (id, name) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, district.getId());
            pstmt.setString(2, district.getName());
            pstmt.executeUpdate();
            if (district.getId() == 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        district.assignId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving district", e);
        }
    }

    /**
     * Inserts one directed edge of the adjacency graph. Callers insert both
     * directions to keep the graph symmetric and queries trivial.
     */
    public void addNeighbor(long districtId, long neighborDistrictId) {
        String sql = "INSERT OR IGNORE INTO district_neighbors (id_district, id_neighbor_district) " +
                "VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, districtId);
            pstmt.setLong(2, neighborDistrictId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving district neighbor", e);
        }
    }

    @Override
    public Set<Long> findNeighborIds(long districtId) {
        String sql = "SELECT id_neighbor_district FROM district_neighbors WHERE id_district = ?";
        Set<Long> neighbors = new HashSet<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, districtId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    neighbors.add(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding district neighbors", e);
        }
        return neighbors;
    }

    @Override
    public Optional<District> findById(long id) {
        String sql = "SELECT * FROM districts WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readDistrict(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding district", e);
        }
        return Optional.empty();
    }

    @Override
    public List<District> findAll() {
        String sql = "SELECT * FROM districts ORDER BY id";
        List<District> districts = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                districts.add(readDistrict(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all districts", e);
        }
        return districts;
    }

    @Override
    public void delete(long id) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement both = connection.prepareStatement(
                    "DELETE FROM district_neighbors WHERE id_district = ? OR id_neighbor_district = ?")) {
                both.setLong(1, id);
                both.setLong(2, id);
                both.executeUpdate();
            }
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "DELETE FROM districts WHERE id = ?")) {
                pstmt.setLong(1, id);
                pstmt.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            rollback(e);
            throw new RuntimeException("Error deleting district", e);
        } finally {
            restoreAutoCommit();
        }
    }

    private void rollback(SQLException cause) {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            throw new RuntimeException("Error rolling back transaction", ex);
        }
    }

    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Error resetting auto-commit", e);
        }
    }

    private District readDistrict(ResultSet rs) throws SQLException {
        District district = new District(rs.getString("name"));
        district.assignId(rs.getLong("id"));
        return district;
    }
}
