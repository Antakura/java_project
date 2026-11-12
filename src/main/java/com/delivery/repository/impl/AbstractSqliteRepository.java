package com.delivery.repository.impl;

import com.delivery.model.AbstractEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC scaffolding for every SQLite repository (DRY).
 *
 * <p>The seven repositories used to repeat the same connection handling,
 * insert-or-replace {@code save}, generated-key readback, {@code findById}/
 * {@code findAll}/{@code delete} skeletons and transaction helpers. All of that
 * lives here once; a concrete repository only supplies the entity-specific hooks
 * ({@link #table()}, {@link #insertColumns()}, {@link #bindFields}, {@link #readRow})
 * and any custom finders, which delegate to the protected query helpers.</p>
 *
 * <p>Error messages are preserved verbatim from the original implementations:
 * the singular {@code entityLabel} drives "Error saving/finding/deleting &lt;label&gt;"
 * and the plural adds an "s" ("Error finding all &lt;label&gt;s").</p>
 *
 * <p>Two helper families are provided: the <i>raw</i> helpers ({@link #executeUpdate},
 * {@link #executeInsert}) propagate {@link SQLException} so they can run inside
 * {@link #inTransaction}; the <i>wrapping</i> helpers ({@link #update},
 * {@link #insertReturningKey}, {@link #queryOne}, {@link #queryList}) translate it
 * into a {@code RuntimeException} with the repository's error message.</p>
 *
 * @param <T> the persisted entity type
 */
public abstract class AbstractSqliteRepository<T extends AbstractEntity> {

    /** Maps the current row of a result set to a domain object. */
    @FunctionalInterface
    protected interface RowMapper<R> {
        R map(ResultSet rs) throws SQLException;
    }

    /** Binds the parameters of a prepared statement. */
    @FunctionalInterface
    protected interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    /** A unit of work executed inside a transaction. */
    @FunctionalInterface
    protected interface SqlWork {
        void run() throws SQLException;
    }

    protected final Connection connection;
    private final String entityLabel;

    protected AbstractSqliteRepository(Connection connection, String entityLabel) {
        this.connection = connection;
        this.entityLabel = entityLabel;
    }

    // --- Entity-specific hooks ------------------------------------------------

    /** The table name, e.g. {@code "clients"}. */
    protected abstract String table();

    /** Comma-separated insert columns, excluding the generated {@code id}. */
    protected abstract String insertColumns();

    /** Binds the non-id columns starting at {@code offset} (1 for insert, 2 for replace). */
    protected abstract void bindFields(PreparedStatement ps, T entity, int offset) throws SQLException;

    /** Rehydrates one entity from the current row. */
    protected abstract T readRow(ResultSet rs) throws SQLException;

    // --- Generic CRUD ---------------------------------------------------------

    public void save(T entity) {
        String columns = insertColumns();
        int columnCount = countColumns(columns);
        String error = "Error saving " + entityLabel;
        if (entity.getId() == 0) {
            // New entity: omit the id so SQLite generates the AUTOINCREMENT key.
            String sql = "INSERT INTO " + table() + " (" + columns + ") VALUES ("
                    + placeholders(columnCount) + ")";
            entity.assignId(insertReturningKey(sql, ps -> bindFields(ps, entity, 1), error));
        } else {
            String sql = "INSERT OR REPLACE INTO " + table() + " (id, " + columns + ") VALUES ("
                    + placeholders(columnCount + 1) + ")";
            update(sql, ps -> {
                ps.setLong(1, entity.getId());
                bindFields(ps, entity, 2);
            }, error);
        }
    }

    public Optional<T> findById(long id) {
        return queryOne("SELECT * FROM " + table() + " WHERE id = ?",
                ps -> ps.setLong(1, id), this::readRow, "Error finding " + entityLabel);
    }

    public List<T> findAll() {
        return queryList("SELECT * FROM " + table() + " ORDER BY id",
                ps -> { }, this::readRow, "Error finding all " + entityLabel + "s");
    }

    public void delete(long id) {
        update("DELETE FROM " + table() + " WHERE id = ?",
                ps -> ps.setLong(1, id), "Error deleting " + entityLabel);
    }

    // --- Wrapping query helpers (standalone statements) ----------------------

    /** Runs a query expected to return at most one row. */
    protected <R> Optional<R> queryOne(String sql, SqlBinder binder, RowMapper<R> mapper, String error) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(error, e);
        }
        return Optional.empty();
    }

    /** Runs a query and maps every row into a list. */
    protected <R> List<R> queryList(String sql, SqlBinder binder, RowMapper<R> mapper, String error) {
        List<R> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(error, e);
        }
        return result;
    }

    /** Executes an INSERT/UPDATE/DELETE that returns no generated key. */
    protected void update(String sql, SqlBinder binder, String error) {
        try {
            executeUpdate(sql, binder);
        } catch (SQLException e) {
            throw new RuntimeException(error, e);
        }
    }

    /** Executes an INSERT and returns the generated primary key. */
    protected long insertReturningKey(String sql, SqlBinder binder, String error) {
        try {
            return executeInsert(sql, binder);
        } catch (SQLException e) {
            throw new RuntimeException(error, e);
        }
    }

    // --- Raw helpers (propagate SQLException for use inside a transaction) ---

    /** Executes an INSERT/UPDATE/DELETE; caller handles {@link SQLException}. */
    protected void executeUpdate(String sql, SqlBinder binder) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            ps.executeUpdate();
        }
    }

    /** Executes an INSERT and returns the generated key; caller handles {@link SQLException}. */
    protected long executeInsert(String sql, SqlBinder binder) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated key for a new " + entityLabel);
                }
                return keys.getLong(1);
            }
        }
    }

    /**
     * Runs {@code work} inside a single transaction: disable auto-commit, commit on
     * success, roll back and rethrow on failure, and always restore auto-commit.
     *
     * <p>All repositories share one connection, so transactions are serialized on
     * it; otherwise two concurrent requests could interleave commit/rollback.</p>
     */
    protected void inTransaction(String error, SqlWork work) {
        synchronized (connection) {
            try {
                connection.setAutoCommit(false);
                work.run();
                connection.commit();
            } catch (SQLException e) {
                rollback();
                throw new RuntimeException(error, e);
            } catch (RuntimeException e) {
                // Without an explicit rollback the finally-block's setAutoCommit(true)
                // would implicitly commit the partial work.
                rollback();
                throw e;
            } finally {
                restoreAutoCommit();
            }
        }
    }

    /** Reads a nullable BIGINT column as a boxed {@code Long} (null when SQL NULL). */
    protected static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private void rollback() {
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

    private static int countColumns(String columns) {
        return columns.split(",").length;
    }

    private static String placeholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('?');
        }
        return sb.toString();
    }
}