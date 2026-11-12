package com.delivery.repository.impl;

import com.delivery.model.Courier;
import com.delivery.repository.CourierRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * SQLite implementation of {@link CourierRepository}.
 */
public class SqliteCourierRepository extends AbstractSqliteRepository<Courier> implements CourierRepository {

    public SqliteCourierRepository(Connection connection) {
        super(connection, "courier");
    }

    @Override
    protected String table() {
        return "couriers";
    }

    @Override
    protected String insertColumns() {
        return "id_user, id_current_district, is_on_shift";
    }

    @Override
    protected void bindFields(PreparedStatement ps, Courier courier, int offset) throws SQLException {
        ps.setLong(offset, courier.getIdUser());
        if (courier.getCurrentDistrictId() == null) {
            ps.setNull(offset + 1, java.sql.Types.INTEGER);
        } else {
            ps.setLong(offset + 1, courier.getCurrentDistrictId());
        }
        ps.setBoolean(offset + 2, courier.isOnShift());
    }

    @Override
    protected Courier readRow(ResultSet rs) throws SQLException {
        return Courier.rehydrate(
                rs.getLong("id"),
                rs.getLong("id_user"),
                nullableLong(rs, "id_current_district"),
                rs.getBoolean("is_on_shift"));
    }

    @Override
    public Optional<Courier> findByIdUser(long userId) {
        return queryOne("SELECT * FROM couriers WHERE id_user = ?",
                ps -> ps.setLong(1, userId), this::readRow, "Error finding courier by user id");
    }
}