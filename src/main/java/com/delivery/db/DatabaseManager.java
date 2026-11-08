package com.delivery.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager handles the connection to the SQLite database.
 * It also ensures that the necessary tables are created on startup.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:nyam_nyam.db";
    private Connection connection;

    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void initializeDatabase() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Create tables if they don't exist.
            // Note: only users are persisted in SQLite for this educational project;
            // products, restaurants and orders live in in-memory repositories.
            statement.execute("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, name TEXT, phone_number TEXT)");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
