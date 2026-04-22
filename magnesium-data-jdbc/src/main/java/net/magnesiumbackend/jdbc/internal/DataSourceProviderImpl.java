package net.magnesiumbackend.jdbc.internal;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.magnesiumbackend.jdbc.DataSourceProvider;
import net.magnesiumbackend.jdbc.JdbcConfig;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Internal implementation of {@link DataSourceProvider}.
 *
 * <p>Creates and manages a HikariCP connection pool based on {@link JdbcConfig}.</p>
 */
public class DataSourceProviderImpl implements DataSourceProvider, AutoCloseable {

    private final JdbcConfig config;
    private volatile HikariDataSource dataSource;
    private volatile boolean initialized = false;

    public DataSourceProviderImpl(JdbcConfig config) {
        this.config = config;
    }

    @Override
    public Optional<DataSource> getDataSource() {
        initializeIfNeeded();
        return Optional.ofNullable(dataSource);
    }

    @Override
    public JdbcConfig getConfig() {
        return config;
    }

    private void initializeIfNeeded() {
        if (initialized) return;

        synchronized (this) {
            if (initialized) return;

            if (config.url() == null || config.url().isBlank()) {
                // No JDBC configured - return empty
                initialized = true;
                return;
            }

            this.dataSource = createHikariDataSource(config);
            this.initialized = true;
        }
    }

    private HikariDataSource createHikariDataSource(JdbcConfig config) {
        HikariConfig hikari = new HikariConfig();

        // Basic connection settings
        hikari.setJdbcUrl(config.url());
        if (config.username() != null) {
            hikari.setUsername(config.username());
        }
        if (config.password() != null) {
            hikari.setPassword(config.password());
        }
        if (config.driverClassName() != null) {
            hikari.setDriverClassName(config.driverClassName());
        }
        if (config.schema() != null) {
            hikari.setSchema(config.schema());
        }

        // Pool settings
        hikari.setMaximumPoolSize(config.maximumPoolSize());
        hikari.setMinimumIdle(config.minimumIdle());
        hikari.setConnectionTimeout(config.connectionTimeout().toMillis());
        hikari.setIdleTimeout(config.idleTimeout().toMillis());
        hikari.setMaxLifetime(config.maxLifetime().toMillis());
        hikari.setLeakDetectionThreshold(config.leakDetectionThreshold().toMillis());
        hikari.setPoolName(config.poolName());
        hikari.setAutoCommit(config.autoCommit());
        hikari.setReadOnly(config.readOnly());

        if (config.transactionIsolation() != null) {
            hikari.setTransactionIsolation(config.transactionIsolation());
        }
        if (config.connectionTestQuery() != null) {
            hikari.setConnectionTestQuery(config.connectionTestQuery());
        }

        // Additional properties
        config.properties().forEach((key, value) -> {
            if (value != null) {
                hikari.addDataSourceProperty(key, value.toString());
            }
        });

        return new HikariDataSource(hikari);
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        initialized = false;
    }
}
