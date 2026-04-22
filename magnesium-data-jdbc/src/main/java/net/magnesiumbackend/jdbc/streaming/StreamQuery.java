package net.magnesiumbackend.jdbc.streaming;

import net.magnesiumbackend.jdbc.RowMapper;

import java.util.stream.Stream;

/**
 * Streaming query API for large result sets.
 *
 * <p>Provides cursor-based streaming with constant memory usage,
 * safe for millions of rows.</p>
 *
 * @see #forEach(RowMapper, ThrowingConsumer) recommended default
 * @see #iterator(RowMapper) for manual iteration
 * @see #stream(RowMapper) for Java Stream API (use with caution)
 */
public interface StreamQuery {

    /**
     * Binds a value to a named parameter.
     *
     * @param name the parameter name
     * @param value the value to bind
     * @return this stream query for chaining
     */
    StreamQuery bind(String name, Object value);

    /**
     * Binds a value to a positional parameter (1-indexed).
     *
     * @param index the parameter index (1-based)
     * @param value the value to bind
     * @return this stream query for chaining
     */
    StreamQuery bind(int index, Object value);

    /**
     * Sets the fetch size hint for the JDBC driver.
     *
     * @param size the fetch size
     * @return this stream query for chaining
     */
    StreamQuery fetchSize(int size);

    /**
     * Marks the query as read-only (optimization hint).
     *
     * @return this stream query for chaining
     */
    StreamQuery readOnly();

    /**
     * Executes the query and streams results to the consumer.
     *
     * <p>This is the <strong>recommended default</strong> for streaming:
     * <ul>
     *   <li>Constant memory</li>
     *   <li>Cursor-based</li>
     *   <li>Safe for millions of rows</li>
     * </ul></p>
     *
     * @param mapper the row mapper
     * @param consumer the consumer for each row
     * @param <T> the result type
     */
    <T> void forEach(RowMapper<T> mapper, ThrowingConsumer<T> consumer);

    /**
     * Executes the query and returns a Java Stream.
     *
     * <p><strong>Warning:</strong> User might accidentally collect,
     * causing memory blowup. Keep try-with-resources scope tight.</p>
     *
     * @param mapper the row mapper
     * @param <T> the result type
     * @return a stream of mapped results
     */
    <T> Stream<T> stream(RowMapper<T> mapper);

    /**
     * Executes the query and returns a closeable iterator.
     *
     * <p>Must be used with try-with-resources to ensure proper cleanup.</p>
     *
     * @param mapper the row mapper
     * @param <T> the result type
     * @return a closeable iterator
     */
    <T> CloseableIterator<T> iterator(RowMapper<T> mapper);
}
