package net.magnesiumbackend.jdbc;

import net.magnesiumbackend.jdbc.internal.InternalJdbcClient;

import javax.sql.DataSource;

/**
 * Factory for creating {@link JdbcClient} instances.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * DataSource dataSource = ...;
 * JdbcClient jdbc = JdbcClientFactory.create(dataSource);
 *
 * // Execute a query
 * List<User> users = jdbc.query("SELECT * FROM users")
 *     .list((rs, rowNum) -> new User(rs.getString("name")));
 *
 * // Stream large results
 * jdbc.stream("SELECT * FROM events")
 *     .fetchSize(1000)
 *     .forEach(eventMapper, event -> process(event));
 *
 * // Transaction
 * Integer count = jdbc.inTransaction(tx -> {
 *     tx.update("INSERT INTO logs VALUES (?)")
 *         .bind(1, "started")
 *         .execute();
 *     return tx.query("SELECT COUNT(*) FROM logs")
 *         .one((rs, row) -> rs.getInt(1));
 * });
 * }</pre>
 */
public final class JdbcClientFactory {

    private JdbcClientFactory() {
        // utility class
    }

    /**
     * Creates a new JdbcClient for the given DataSource.
     *
     * @param dataSource the data source
     * @return a new JdbcClient instance
     */
    public static JdbcClient create(DataSource dataSource) {
        return new InternalJdbcClient(dataSource);
    }
}
