package com.delivery.repository.impl;

import com.delivery.model.District;
import com.delivery.repository.DistrictRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * SQLite implementation of {@link DistrictRepository}, including the
 * {@code district_neighbors} adjacency graph.
 */
public class SqliteDistrictRepository extends AbstractSqliteRepository<District> implements DistrictRepository {

    public SqliteDistrictRepository(Connection connection) {
        super(connection, "district");
    }

    @Override
    protected String table() {
        return "districts";
    }

    @Override
    protected String insertColumns() {
        return "name";
    }

    @Override
    protected void bindFields(PreparedStatement ps, District district, int offset) throws SQLException {
        ps.setString(offset, district.getName());
    }

    @Override
    protected District readRow(ResultSet rs) throws SQLException {
        District district = new District(rs.getString("name"));
        district.assignId(rs.getLong("id"));
        return district;
    }

    /**
     * Inserts one directed edge of the adjacency graph. Callers insert both
     * directions to keep the graph symmetric and queries trivial.
     */
    public void addNeighbor(long districtId, long neighborDistrictId) {
        update("INSERT OR IGNORE INTO district_neighbors (id_district, id_neighbor_district) VALUES (?, ?)",
                ps -> {
                    ps.setLong(1, districtId);
                    ps.setLong(2, neighborDistrictId);
                }, "Error saving district neighbor");
    }

    @Override
    public Set<Long> findNeighborIds(long districtId) {
        return new HashSet<>(queryList(
                "SELECT id_neighbor_district FROM district_neighbors WHERE id_district = ?",
                ps -> ps.setLong(1, districtId), rs -> rs.getLong(1), "Error finding district neighbors"));
    }

    @Override
    public void delete(long id) {
        // Remove the graph edges first, then the district — one atomic unit.
        inTransaction("Error deleting district", () -> {
            executeUpdate("DELETE FROM district_neighbors WHERE id_district = ? OR id_neighbor_district = ?",
                    ps -> {
                        ps.setLong(1, id);
                        ps.setLong(2, id);
                    });
            executeUpdate("DELETE FROM districts WHERE id = ?", ps -> ps.setLong(1, id));
        });
    }
}