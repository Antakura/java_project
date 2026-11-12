package com.delivery.repository.impl;

import com.delivery.model.Courier;
import com.delivery.repository.CourierRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link CourierRepository}.
 */
public class SqliteCourierRepository implements CourierRepository {
    private final Connection connection;

    public SqliteCourierRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(Courier courier) {
        String sql = "INSERT OR REPLACE INTO couriers (id, id_user, id_current_district, is_on_shift) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, courier.getId());
            pstmt.setLong(2, courier.getIdUser());
            if (courier.getCurrentDistrictId() == null) {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            } else {
                pstmt.setLong(3, courier.getCurrentDistrictId());
            }
            pstmt.setBoolean(4, courier.isOnShift());
            pstmt.executeUpdate();
            if (courier.getId() == 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        courier.assignId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving courier", e);
        }
    }

    @Override
    public Optional<Courier> findById(long id) {
        String sql = "SELECT * FROM couriers WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readCourier(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding courier", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Courier> findByIdUser(long userId) {
        String sql = "SELECT * FROM couriers WHERE id_user = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readCourier(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding courier by user id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Courier> findAll() {
        String sql = "SELECT * FROM couriers ORDER BY id";
        List<Courier> couriers = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                couriers.add(readCourier(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all couriers", e);
        }
        return couriers;
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM couriers WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting courier", e);
        }
    }

    private Courier readCourier(ResultSet rs) throws SQLException {
        long districtId = rs.getLong("id_current_district");
        Long nullableDistrict = rs.wasNull() ? null : districtId;
        return Courier.rehydrate(
                rs.getLong("id"),
                rs.getLong("id_user"),
                nullableDistrict,
                rs.getBoolean("is_on_shift"));
    }
}
