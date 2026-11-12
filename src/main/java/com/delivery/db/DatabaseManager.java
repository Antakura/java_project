package com.delivery.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the SQLite connection and the schema lifecycle.
 *
 * <h2>Schema</h2>
 * The DDL below implements the target relational model (1NF/3NF, no JSON blobs,
 * no mixed-role columns):
 * <pre>
 *   users(id, username, password_hash, role)
 *   clients(id, id_user FK, phone, default_address)
 *   couriers(id, id_user FK, id_current_district FK nullable, is_on_shift)
 *   restaurants(id, name, address, id_district FK)
 *   products(id, name, description, price, image_url, is_available, id_restaurant FK)
 *   districts(id, name)
 *   district_neighbors(id_district FK, id_neighbor_district FK)  -- adjacency graph
 *   orders(id, id_client FK, id_restaurant FK, id_courier FK nullable,
 *          status, total_price, delivery_address, created_at)
 *   order_items(id, id_order FK, id_product FK, quantity, price_at_order)
 * </pre>
 *
 * <h2>Migration strategy</h2>
 * A {@code schema_version} table records which DDL generation is in place. If the
 * stored version differs from {@link #SCHEMA_VERSION}, all tables are dropped and
 * recreated (and re-seeded by {@code Main}). For an educational project this
 * "reset on bump" approach is simpler and safer than a chain of ALTER scripts,
 * and it cleanly handles the legacy database file that shipped with the old schema.
 */
public class DatabaseManager {
    /** Bump this number whenever the DDL below changes. */
    private static final int SCHEMA_VERSION = 3;

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:nyam_nyam.db";

    private final Connection connection;

    public DatabaseManager() {
        this(DEFAULT_DB_URL);
    }

    /**
     * @param jdbcUrl any SQLite URL, e.g. {@code jdbc:sqlite::memory:} for tests.
     */
    public DatabaseManager(String jdbcUrl) {
        try {
            connection = DriverManager.getConnection(jdbcUrl);
            // Enforce referential integrity (SQLite has it off by default).
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            migrate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void migrate() throws SQLException {
        int storedVersion = readSchemaVersion();
        if (storedVersion != SCHEMA_VERSION) {
            System.out.println("[DB] Schema version " + storedVersion
                    + " != " + SCHEMA_VERSION + ": resetting database.");
            dropAllTables();
            createSchema();
            writeSchemaVersion(SCHEMA_VERSION);
        }
    }

    private int readSchemaVersion() throws SQLException {
        // The table may not exist on a brand-new file.
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='schema_version'")) {
            if (!rs.next()) {
                return 0;
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT version FROM schema_version LIMIT 1")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void writeSchemaVersion(int version) throws SQLException {
        try (PreparedStatement pstmt =
                     connection.prepareStatement("INSERT INTO schema_version(version) VALUES (?)")) {
            pstmt.setInt(1, version);
            pstmt.executeUpdate();
        }
    }

    /** Drop order respects foreign keys (children first). */
    private void dropAllTables() throws SQLException {
        String[] tables = {"order_items", "orders", "district_neighbors", "products",
                "restaurants", "couriers", "clients", "users", "districts", "schema_version"};
        try (Statement statement = connection.createStatement()) {
            for (String table : tables) {
                statement.execute("DROP TABLE IF EXISTS " + table);
            }
        }
    }

    private void createSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "username TEXT NOT NULL UNIQUE, " +
                            "password_hash TEXT NOT NULL, " +
                            "role TEXT NOT NULL CHECK(role IN ('client', 'admin', 'courier')))");

            statement.execute(
                    "CREATE TABLE clients (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "id_user INTEGER NOT NULL UNIQUE REFERENCES users(id), " +
                            "phone TEXT NOT NULL, " +
                            "default_address TEXT NOT NULL)");

            statement.execute(
                    "CREATE TABLE couriers (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "id_user INTEGER NOT NULL UNIQUE REFERENCES users(id), " +
                            "id_current_district INTEGER REFERENCES districts(id), " +
                            "is_on_shift BOOLEAN NOT NULL DEFAULT 0)");

            statement.execute(
                    "CREATE TABLE districts (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL UNIQUE)");

            // Adjacency graph of districts: symmetric edges are stored explicitly.
            statement.execute(
                    "CREATE TABLE district_neighbors (" +
                            "id_district INTEGER NOT NULL REFERENCES districts(id), " +
                            "id_neighbor_district INTEGER NOT NULL REFERENCES districts(id), " +
                            "PRIMARY KEY (id_district, id_neighbor_district))");

            statement.execute(
                    "CREATE TABLE restaurants (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL, " +
                            "address TEXT NOT NULL, " +
                            "id_district INTEGER NOT NULL REFERENCES districts(id))");

            statement.execute(
                    "CREATE TABLE products (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL, " +
                            "description TEXT NOT NULL DEFAULT '', " +
                            "price REAL NOT NULL CHECK(price > 0), " +
                            "image_url TEXT NOT NULL DEFAULT '', " +
                            "is_available BOOLEAN NOT NULL DEFAULT 1, " +
                            "id_restaurant INTEGER NOT NULL REFERENCES restaurants(id))");

            statement.execute(
                    "CREATE TABLE orders (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "id_client INTEGER NOT NULL REFERENCES clients(id), " +
                            "id_restaurant INTEGER NOT NULL REFERENCES restaurants(id), " +
                            "id_courier INTEGER REFERENCES couriers(id), " +
                            "status TEXT NOT NULL, " +
                            "total_price REAL NOT NULL, " +
                            "delivery_address TEXT NOT NULL, " +
                            "created_at TEXT NOT NULL)");

            statement.execute(
                    "CREATE TABLE order_items (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "id_order INTEGER NOT NULL REFERENCES orders(id), " +
                            "id_product INTEGER NOT NULL REFERENCES products(id), " +
                            "quantity INTEGER NOT NULL CHECK(quantity >= 1), " +
                            "price_at_order REAL NOT NULL CHECK(price_at_order > 0))");

            statement.execute(
                    "CREATE TABLE schema_version (" +
                            "version INTEGER NOT NULL)");
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
            System.err.println("[DB] Failed to close connection: " + e.getMessage());
        }
    }
}
