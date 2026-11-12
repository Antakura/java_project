package com.delivery.repository.impl;

import com.delivery.model.Role;
import com.delivery.model.User;
import com.delivery.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * SQLite implementation of {@link UserRepository}.
 */
public class SqliteUserRepository extends AbstractSqliteRepository<User> implements UserRepository {

    public SqliteUserRepository(Connection connection) {
        super(connection, "user");
    }

    @Override
    protected String table() {
        return "users";
    }

    @Override
    protected String insertColumns() {
        return "username, password_hash, role";
    }

    @Override
    protected void bindFields(PreparedStatement ps, User user, int offset) throws SQLException {
        ps.setString(offset, user.getUsername());
        ps.setString(offset + 1, user.getPasswordHash());
        ps.setString(offset + 2, user.getRole().getDbValue());
    }

    @Override
    protected User readRow(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getString("username"),
                rs.getString("password_hash"),
                Role.fromDbValue(rs.getString("role")));
        user.assignId(rs.getLong("id"));
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return queryOne("SELECT * FROM users WHERE username = ?",
                ps -> ps.setString(1, username), this::readRow, "Error finding user by username");
    }
}