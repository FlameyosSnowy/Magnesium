package net.magnesiumbackend.jdbc.internal;

import net.magnesiumbackend.jdbc.RowMapper;
import net.magnesiumbackend.jdbc.Update;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal implementation of {@link Update}.
 */
public class InternalUpdate implements Update {

    private final DataSource dataSource;
    private final String sql;
    private final Map<String, Object> namedParams = new HashMap<>();
    private final Map<Integer, Object> positionalParams = new HashMap<>();

    InternalUpdate(DataSource dataSource, String sql) {
        this.dataSource = dataSource;
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
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            applyBindings(stmt);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + sql, e);
        }
    }

    @Override
    public <T> T executeAndReturnKey(RowMapper<T> keyMapper) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

    private void applyBindings(PreparedStatement stmt) throws SQLException {
        for (Map.Entry<Integer, Object> entry : positionalParams.entrySet()) {
            stmt.setObject(entry.getKey(), entry.getValue());
        }
    }
}
