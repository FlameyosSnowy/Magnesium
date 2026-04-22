package net.magnesium.universal;

import io.github.flameyossnowy.universal.sql.internals.SQLConnectionProvider;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Simple wrapper around a {@link DataSource} that implements
 * {@link SQLConnectionProvider} for Universal ORM compatibility.
 *
 * <p>This allows using Magnesium's configured DataSource with the
 * Universal ORM without requiring custom connection provider implementations.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DataSource dataSource = // from Magnesium JDBC extension
 * SQLConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
 *
 * // Use with Universal ORM builder
 * MySQLRepositoryAdapter.<User, UUID>builder(User.class, UUID.class)
 *     .withConnectionProvider((_, _) -> provider)
 *     .build();
 * }</pre>
 */
public record DataSourceConnectionProvider(@NotNull DataSource dataSource) implements SQLConnectionProvider {

    public DataSourceConnectionProvider {
        Objects.requireNonNull(dataSource, "DataSource cannot be null");
    }

    @Override
    public @NotNull Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        // DataSource is managed by Magnesium's JDBC extension
        // We don't close it here to avoid interfering with connection pooling
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    @Override
    public PreparedStatement prepareStatement(String sql, Connection connection) throws Exception {
        return connection.prepareStatement(sql);
    }

    /**
     * Returns the underlying DataSource.
     */
    @Override
    public @NotNull DataSource dataSource() {
        return dataSource;
    }
}
