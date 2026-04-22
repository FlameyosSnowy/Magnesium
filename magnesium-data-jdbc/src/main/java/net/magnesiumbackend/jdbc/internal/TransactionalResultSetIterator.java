package net.magnesiumbackend.jdbc.internal;

import net.magnesiumbackend.jdbc.RowMapper;
import net.magnesiumbackend.jdbc.streaming.CloseableIterator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Iterator over a ResultSet within a transaction.
 *
 * <p>Does not close the connection when done (transaction manages it).</p>
 *
 * @param <T> the element type
 */
class TransactionalResultSetIterator<T> implements CloseableIterator<T> {

    private final Connection connection;
    private final String sql;
    private final int fetchSize;
    private final Map<Integer, Object> params;
    private final RowMapper<T> mapper;

    private PreparedStatement statement;
    private ResultSet resultSet;
    private boolean hasNext;
    private int rowNum = 0;
    private boolean closed = false;

    TransactionalResultSetIterator(Connection connection, String sql, int fetchSize,
                                   Map<Integer, Object> params, RowMapper<T> mapper) {
        this.connection = connection;
        this.sql = sql;
        this.fetchSize = fetchSize;
        this.params = params;
        this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
        ensureOpen();
        if (resultSet == null) {
            try {
                init();
                advance();
            } catch (SQLException e) {
                close();
                throw new RuntimeException("Failed to execute query: " + sql, e);
            }
        }
        return hasNext;
    }

    @Override
    public T next() {
        ensureOpen();
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        try {
            T result = mapper.mapRow(resultSet, ++rowNum);
            advance();
            return result;
        } catch (SQLException e) {
            close();
            throw new RuntimeException("Failed to map row", e);
        }
    }

    private void advance() throws SQLException {
        hasNext = resultSet.next();
        if (!hasNext) {
            close();
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Iterator is closed");
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        // Only close statement and result set, not the connection
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private void init() throws SQLException {
        statement = connection.prepareStatement(sql);
        statement.setFetchSize(fetchSize);

        for (Map.Entry<Integer, Object> entry : params.entrySet()) {
            statement.setObject(entry.getKey(), entry.getValue());
        }

        resultSet = statement.executeQuery();
    }
}
