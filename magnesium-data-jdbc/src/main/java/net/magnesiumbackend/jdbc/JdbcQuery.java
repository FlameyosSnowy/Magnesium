package net.magnesiumbackend.jdbc;

import java.util.List;
import java.util.Optional;

/**
 * Query API for non-streaming JDBC operations.
 *
 * <p>This is for <strong>small/medium result sets only</strong>.</p>
 *
 * @see net.magnesiumbackend.jdbc.streaming.StreamQuery for large result sets
 */
public interface JdbcQuery {

    /**
     * Binds a value to a named parameter.
     *
     * @param name the parameter name
     * @param value the value to bind
     * @return this query for chaining
     */
    JdbcQuery bind(String name, Object value);

    /**
     * Binds a value to a positional parameter (1-indexed).
     *
     * @param index the parameter index (1-based)
     * @param value the value to bind
     * @return this query for chaining
     */
    JdbcQuery bind(int index, Object value);

    /**
     * Executes the query and maps all results.
     *
     * @param mapper the row mapper
     * @param <T> the result type
     * @return list of mapped results
     */
    <T> List<T> list(RowMapper<T> mapper);

    /**
     * Executes the query and returns exactly one result.
     *
     * @param mapper the row mapper
     * @param <T> the result type
     * @return the mapped result
     * @throws java.util.NoSuchElementException if no result found
     * @throws IllegalStateException if more than one result found
     */
    <T> T one(RowMapper<T> mapper);

    /**
     * Executes the query and returns zero or one result.
     *
     * @param mapper the row mapper
     * @param <T> the result type
     * @return optional mapped result
     * @throws IllegalStateException if more than one result found
     */
    <T> Optional<T> optional(RowMapper<T> mapper);
}
