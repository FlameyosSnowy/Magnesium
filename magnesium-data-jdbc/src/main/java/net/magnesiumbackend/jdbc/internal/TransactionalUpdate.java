package net.magnesiumbackend.jdbc.internal;

import net.magnesiumbackend.jdbc.RowMapper;
import net.magnesiumbackend.jdbc.Update;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Update implementation for use within a transaction.
 */
class TransactionalUpdate implements Update {

    private final Connection connection;
    private final String sql;
    private final Map<String, Object> namedParams = new HashMap<>();
    private final Map<Integer, Object> positionalParams = new HashMap<>();

    TransactionalUpdate(Connection connection, String sql) {
        this.connection = connection;
        this.sql = sql;
    }

    @Override
    public Update bind(String name, Object value) {
        namedParams.put(name, value);
        return this;
    }

    @Override
    public Update bind(int index, Object value) {
        positionalParams.put(index, value);
        return this;
    }

    @Override
    public int execute() {
        try (PreparedStatement stmt = prepareStatement()) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + sql, e);
        }
    }

    @Override
    public <T> T executeAndReturnKey(RowMapper<T> keyMapper) {
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            applyBindings(stmt);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return keyMapper.mapRow(rs, 1);
                }
                throw new IllegalStateException("No generated key returned");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + sql, e);
        }
    }

    private PreparedStatement prepareStatement() throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(sql);
        applyBindings(stmt);
        return stmt;
    }

    private void applyBindings(PreparedStatement stmt) throws SQLException {
        for (Map.Entry<Integer, Object> entry : positionalParams.entrySet()) {
            stmt.setObject(entry.getKey(), entry.getValue());
        }
    }
}
