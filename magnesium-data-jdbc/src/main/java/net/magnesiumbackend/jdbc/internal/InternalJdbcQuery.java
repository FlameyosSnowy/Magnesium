package net.magnesiumbackend.jdbc.internal;

import net.magnesiumbackend.jdbc.JdbcQuery;
import net.magnesiumbackend.jdbc.RowMapper;

import javax.sql.DataSource;
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
 * Internal implementation of {@link JdbcQuery}.
 */
public class InternalJdbcQuery implements JdbcQuery {

    private final DataSource dataSource;
    private final String sql;
    private final Map<String, Object> namedParams = new HashMap<>();
    private final Map<Integer, Object> positionalParams = new HashMap<>();

    InternalJdbcQuery(DataSource dataSource, String sql) {
        this.dataSource = dataSource;
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
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = prepareStatement(conn);
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

    private PreparedStatement prepareStatement(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        applyBindings(stmt);
        return stmt;
    }

    void applyBindings(PreparedStatement stmt) throws SQLException {
        for (Map.Entry<Integer, Object> entry : positionalParams.entrySet()) {
            stmt.setObject(entry.getKey(), entry.getValue());
        }
        // Named params would require parsing SQL - simplified here
    }

    String getSql() {
        return sql;
    }
}
