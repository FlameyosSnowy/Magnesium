package net.magnesiumbackend.jdbc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * JDBC configuration foundation for Magnesium and extensions.
 *
 * <p>Provides configuration for:
 * <ul>
 *   <li>Connection pooling (HikariCP)</li>
 *   <li>Database connection properties</li>
 *   <li>Extension-specific settings (Hibernate, caching, etc.)</li>
 * </ul></p>
 *
 * <p>Extensions can access this config via {@code runtime.configurationManager().get(JdbcConfig.class)}
 * and register their own DataSource-aware services.</p>
 *
 * <h3>Example configuration (application.toml)</h3>
 * <pre>{@code
 * [jdbc]
 * url = "jdbc:postgresql://localhost:5432/mydb"
 * username = "postgres"
 * password = "secret"
 * driver-class-name = "org.postgresql.Driver"
 *
 * [jdbc.pool]
 * maximum-pool-size = 20
 * minimum-idle = 5
 * connection-timeout = "30s"
 * idle-timeout = "10m"
 * max-lifetime = "30m"
 * leak-detection-threshold = "5s"
 *
 * [jdbc.extensions.hibernate]
 * ddl-auto = "update"
 * show-sql = false
 * cache.use-second-level-cache = true
 * cache.region.factory-class = "org.hibernate.cache.jcache.JCacheRegionFactory"
 * }</pre>
 */
public final class JdbcConfig {

    // Connection settings
    private final String url;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final String schema;

    // Pool settings
    private final int maximumPoolSize;
    private final int minimumIdle;
    private final Duration connectionTimeout;
    private final Duration idleTimeout;
    private final Duration maxLifetime;
    private final Duration leakDetectionThreshold;
    private final String poolName;
    private final boolean autoCommit;
    private final boolean readOnly;
    private final String transactionIsolation;
    private final String connectionTestQuery;

    // Extension settings
    private final Map<String, Object> properties;
    private final Map<String, Map<String, Object>> extensionConfigs;

    private JdbcConfig(Builder b) {
        this.url = b.url;
        this.username = b.username;
        this.password = b.password;
        this.driverClassName = b.driverClassName;
        this.schema = b.schema;
        this.maximumPoolSize = b.maximumPoolSize;
        this.minimumIdle = b.minimumIdle;
        this.connectionTimeout = b.connectionTimeout;
        this.idleTimeout = b.idleTimeout;
        this.maxLifetime = b.maxLifetime;
        this.leakDetectionThreshold = b.leakDetectionThreshold;
        this.poolName = b.poolName;
        this.autoCommit = b.autoCommit;
        this.readOnly = b.readOnly;
        this.transactionIsolation = b.transactionIsolation;
        this.connectionTestQuery = b.connectionTestQuery;
        this.properties = new HashMap<>(b.properties);
        this.extensionConfigs = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : b.extensionConfigs.entrySet()) {
            String k = entry.getKey();
            Map<String, Object> v = entry.getValue();
            this.extensionConfigs.put(k, new HashMap<>(v));
        }
    }

    /** JDBC URL (e.g., jdbc:postgresql://localhost:5432/mydb) */
    public @Nullable String url() { return url; }

    /** Database username */
    public @Nullable String username() { return username; }

    /** Database password */
    public @Nullable String password() { return password; }

    /** JDBC driver class name (optional, usually auto-detected from URL) */
    public @Nullable String driverClassName() { return driverClassName; }

    /** Default schema to use */
    public @Nullable String schema() { return schema; }

    /** Maximum number of connections in the pool (default: 10) */
    public int maximumPoolSize() { return maximumPoolSize; }

    /** Minimum number of idle connections (default: same as max) */
    public int minimumIdle() { return minimumIdle; }

    /** Timeout for obtaining a connection from the pool (default: 30s) */
    public @NotNull Duration connectionTimeout() { return connectionTimeout; }

    /** Maximum time a connection can sit idle before being retired (default: 10m) */
    public @NotNull Duration idleTimeout() { return idleTimeout; }

    /** Maximum lifetime of a connection (default: 30m) */
    public @NotNull Duration maxLifetime() { return maxLifetime; }

    /** Threshold for logging connection leaks (default: 0 = disabled) */
    public @NotNull Duration leakDetectionThreshold() { return leakDetectionThreshold; }

    /** Name of the connection pool */
    public @Nullable String poolName() { return poolName; }

    /** Default auto-commit behavior for connections */
    public boolean autoCommit() { return autoCommit; }

    /** Whether connections should be read-only by default */
    public boolean readOnly() { return readOnly; }

    /** Transaction isolation level (e.g., TRANSACTION_READ_COMMITTED) */
    public @Nullable String transactionIsolation() { return transactionIsolation; }

    /** Query to test connection validity (if JDBC4 detection fails) */
    public @Nullable String connectionTestQuery() { return connectionTestQuery; }

    /**
     * Gets a property value by key.
     *
     * @param key the property key
     * @return the value, or null if not set
     */
    public @Nullable Object property(String key) {
        return properties.get(key);
    }

    /**
     * Gets all properties as an unmodifiable map.
     */
    public @NotNull Map<String, Object> properties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Gets configuration for a specific extension.
     *
     * @param extensionName the extension name (e.g., "hibernate")
     * @return the extension config map, or empty map if not configured
     */
    public @NotNull Map<String, Object> extensionConfig(String extensionName) {
        return extensionConfigs.getOrDefault(extensionName, Collections.emptyMap());
    }

    /**
     * Gets all extension configs as an unmodifiable map.
     */
    public @NotNull Map<String, Map<String, Object>> extensionConfigs() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        extensionConfigs.forEach((k, v) -> result.put(k, Collections.unmodifiableMap(v)));
        return Collections.unmodifiableMap(result);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        // Connection defaults
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private String schema;

        // Pool defaults (HikariCP defaults)
        private int maximumPoolSize = 10;
        private int minimumIdle = -1; // -1 means same as maximumPoolSize
        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration idleTimeout = Duration.ofMinutes(10);
        private Duration maxLifetime = Duration.ofMinutes(30);
        private Duration leakDetectionThreshold = Duration.ZERO; // disabled by default
        private String poolName;
        private boolean autoCommit = true;
        private boolean readOnly = false;
        private String transactionIsolation;
        private String connectionTestQuery;

        // Extension settings
        private final Map<String, Object> properties = new HashMap<>();
        private final Map<String, Map<String, Object>> extensionConfigs = new HashMap<>();

        private Builder() {}

        // Connection settings
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder driverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
            return this;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        // Pool settings
        public Builder maximumPoolSize(int size) {
            if (size < 1) throw new IllegalArgumentException("maximumPoolSize must be >= 1");
            this.maximumPoolSize = size;
            return this;
        }

        public Builder minimumIdle(int idle) {
            if (idle < 0) throw new IllegalArgumentException("minimumIdle must be >= 0");
            this.minimumIdle = idle;
            return this;
        }

        public Builder connectionTimeout(Duration timeout) {
            this.connectionTimeout = Objects.requireNonNull(timeout);
            return this;
        }

        public Builder idleTimeout(Duration timeout) {
            this.idleTimeout = Objects.requireNonNull(timeout);
            return this;
        }

        public Builder maxLifetime(Duration lifetime) {
            this.maxLifetime = Objects.requireNonNull(lifetime);
            return this;
        }

        public Builder leakDetectionThreshold(Duration threshold) {
            this.leakDetectionThreshold = Objects.requireNonNull(threshold);
            return this;
        }

        public Builder poolName(String name) {
            this.poolName = name;
            return this;
        }

        public Builder autoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder transactionIsolation(String isolation) {
            this.transactionIsolation = isolation;
            return this;
        }

        public Builder connectionTestQuery(String query) {
            this.connectionTestQuery = query;
            return this;
        }

        // Extension settings
        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> props) {
            this.properties.putAll(props);
            return this;
        }

        /**
         * Sets configuration for a specific extension.
         *
         * @param extensionName the extension name
         * @param config the configuration map
         */
        public Builder extensionConfig(String extensionName, Map<String, Object> config) {
            this.extensionConfigs.put(extensionName, new HashMap<>(config));
            return this;
        }

        /**
         * Sets a single extension property.
         *
         * @param extensionName the extension name
         * @param key the property key
         * @param value the property value
         */
        public Builder extensionProperty(String extensionName, String key, Object value) {
            this.extensionConfigs
                .computeIfAbsent(extensionName, k -> new HashMap<>())
                .put(key, value);
            return this;
        }

        public @NotNull JdbcConfig build() {
            if (minimumIdle == -1) {
                minimumIdle = maximumPoolSize;
            }
            if (poolName == null) {
                poolName = "MagnesiumHikariPool";
            }
            return new JdbcConfig(this);
        }
    }

    @Override
    public String toString() {
        return "JdbcConfig{url=" + (url != null ? "***" : null) +
            ", pool=" + maximumPoolSize + " max, " + minimumIdle + " min}";
    }
}
