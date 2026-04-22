package net.magnesiumbackend.jdbc;

import net.magnesiumbackend.jdbc.streaming.StreamQuery;

import java.util.function.Function;

/**
 * Core entry point for JDBC operations.
 *
 * <p>Provides fluent APIs for queries, updates, and streaming operations.</p>
 */
public interface JdbcClient {

    /**
     * Creates a query for the given SQL.
     *
     * @param sql the SQL query
     * @return a JdbcQuery builder
     */
    JdbcQuery query(String sql);

    /**
     * Creates an update for the given SQL.
     *
     * @param sql the SQL update/insert/delete
     * @return an Update builder
     */
    Update update(String sql);

    /**
     * Creates a streaming query for the given SQL.
     *
     * @param sql the SQL query
     * @return a StreamQuery builder
     */
    StreamQuery stream(String sql);

    /**
     * Executes the given function within a transaction.
     *
     * @param fn the function to execute
     * @param <T> the return type
     * @return the result of the function
     */
    <T> T inTransaction(Function<JdbcClient, T> fn);
}
