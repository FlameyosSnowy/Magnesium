package net.magnesiumbackend.jdbc.internal;

import net.magnesiumbackend.jdbc.JdbcQuery;
import net.magnesiumbackend.jdbc.RowMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * JdbcQuery implementation for use within a transaction.
 */
class TransactionalJdbcQuery implements JdbcQuery {

    private final Connection connection;
    private final String sql;
    private final Map<String, Object> namedParams = new HashMap<>();
    private final Map<Integer, Object> positionalParams = new HashMap<>();

    TransactionalJdbcQuery(Connection connection, String sql) {
        this.connection = connection;
        this.sql = sql;
    }

    @Override
    public JdbcQuery bind(String name, Object value) {
        namedParams.put(name, value);
        return this;
    }

    @Override
    public JdbcQuery bind(int index, Object value) {
        positionalParams.put(index, value);
        return this;
    }

    @Override
    public <T> List<T> list(RowMapper<T> mapper) {
        try (PreparedStatement stmt = prepareStatement();
             ResultSet rs = stmt.executeQuery()) {

            List<T> results = new ArrayList<>();
            int rowNum = 0;
            while (rs.next()) {
                rowNum++;
                results.add(mapper.mapRow(rs, rowNum));
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
    }

    @Override
    public <T> T one(RowMapper<T> mapper) {
        List<T> results = list(mapper);
        if (results.isEmpty()) {
            throw new NoSuchElementException("Expected exactly one result, got none");
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Expected exactly one result, got " + results.size());
        }
        return results.getFirst();
    }

    @Override
    public <T> Optional<T> optional(RowMapper<T> mapper) {
        return Optional.of(one(mapper));
    }

    private PreparedStatement prepareStatement() throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(sql);
        for (Map.Entry<Integer, Object> entry : positionalParams.entrySet()) {
            stmt.setObject(entry.getKey(), entry.getValue());
        }
        return stmt;
    }
}
