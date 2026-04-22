package net.magnesiumbackend.jdbc.internal;

import net.magnesiumbackend.jdbc.RowMapper;
import net.magnesiumbackend.jdbc.streaming.CloseableIterator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Iterator over a ResultSet that properly closes resources.
 *
 * @param <T> the element type
 */
public class ResultSetIterator<T> implements CloseableIterator<T> {

    private final DataSource dataSource;
    private final String sql;
    private final int fetchSize;
    private final boolean readOnly;
    private final Map<Integer, Object> params;
    private final RowMapper<T> mapper;

    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private boolean hasNext;
    private int rowNum = 0;
    private boolean closed = false;

    ResultSetIterator(DataSource dataSource, String sql, int fetchSize, boolean readOnly,
                      Map<Integer, Object> params, RowMapper<T> mapper) {
        this.dataSource = dataSource;
        this.sql = sql;
        this.fetchSize = fetchSize;
        this.readOnly = readOnly;
        this.params = params;
        this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
        ensureOpen();
        if (resultSet == null) {
            try {
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

        // Close in reverse order of creation
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
        if (connection != null) {
            try {
                // Commit any remaining changes if not read-only
                if (!connection.getAutoCommit() && !readOnly) {
                    connection.commit();
                }
            } catch (SQLException ignored) {
            }
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private void init() throws SQLException {
        connection = dataSource.getConnection();

        // For streaming, auto-commit must be false
        if (connection.getAutoCommit()) {
            connection.setAutoCommit(false);
        }

        statement = connection.prepareStatement(sql);
        statement.setFetchSize(fetchSize);

        for (Map.Entry<Integer, Object> entry : params.entrySet()) {
            statement.setObject(entry.getKey(), entry.getValue());
        }

        resultSet = statement.executeQuery();
    }
}
