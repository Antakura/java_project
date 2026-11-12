package com.delivery.repository.impl;

import com.delivery.model.Client;
import com.delivery.repository.ClientRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * SQLite implementation of {@link ClientRepository}.
 */
public class SqliteClientRepository extends AbstractSqliteRepository<Client> implements ClientRepository {

    public SqliteClientRepository(Connection connection) {
        super(connection, "client");
    }

    @Override
    protected String table() {
        return "clients";
    }

    @Override
    protected String insertColumns() {
        return "id_user, phone, default_address";
    }

    @Override
    protected void bindFields(PreparedStatement ps, Client client, int offset) throws SQLException {
        ps.setLong(offset, client.getIdUser());
        ps.setString(offset + 1, client.getPhone());
        ps.setString(offset + 2, client.getDefaultAddress());
    }

    @Override
    protected Client readRow(ResultSet rs) throws SQLException {
        Client client = new Client(
                rs.getLong("id_user"),
                rs.getString("phone"),
                rs.getString("default_address"));
        client.assignId(rs.getLong("id"));
        return client;
    }

    @Override
    public Optional<Client> findByIdUser(long userId) {
        return queryOne("SELECT * FROM clients WHERE id_user = ?",
                ps -> ps.setLong(1, userId), this::readRow, "Error finding client by user id");
    }
}