package com.delivery.repository.impl;

import com.delivery.model.Restaurant;
import com.delivery.repository.RestaurantRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SQLite implementation of {@link RestaurantRepository}.
 */
public class SqliteRestaurantRepository extends AbstractSqliteRepository<Restaurant> implements RestaurantRepository {

    public SqliteRestaurantRepository(Connection connection) {
        super(connection, "restaurant");
    }

    @Override
    protected String table() {
        return "restaurants";
    }

    @Override
    protected String insertColumns() {
        return "name, address, id_district";
    }

    @Override
    protected void bindFields(PreparedStatement ps, Restaurant restaurant, int offset) throws SQLException {
        ps.setString(offset, restaurant.getName());
        ps.setString(offset + 1, restaurant.getAddress());
        ps.setLong(offset + 2, restaurant.getDistrictId());
    }

    @Override
    protected Restaurant readRow(ResultSet rs) throws SQLException {
        Restaurant restaurant = new Restaurant(
                rs.getString("name"),
                rs.getString("address"),
                rs.getLong("id_district"));
        restaurant.assignId(rs.getLong("id"));
        return restaurant;
    }
}