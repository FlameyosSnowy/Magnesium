package net.magnesium.universal;

import io.github.flameyossnowy.universal.api.Optimizations;
import io.github.flameyossnowy.universal.sql.query.SQLQueryValidator;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.jdbc.DataSourceAware;
import net.magnesiumbackend.jdbc.JdbcConfig;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataSourceAware extension for Universal ORM integration with Magnesium.
 *
 * <p>Auto-configures RepositoryAdapters for multiple entity types using a shared
 * DataSource from the Magnesium JDBC extension. Supports MySQL, PostgreSQL, and
 * SQLite databases based on the JDBC URL.</p>
 *
 * <h3>Configuration (application.toml)</h3>
 * <pre>{@code
 * [jdbc]
 * url = "jdbc:mysql://localhost:3306/mydb"
 * username = "root"
 * password = "secret"
 *
 * [jdbc.extensions.universal]
 * dialect = "mysql"  # auto-detected from URL if not specified
 * optimizations = "recommended"  # or "none", "aggressive"
 * }</pre>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // In your Application.configure():
 * runtime.services(services -> {
 *     // Register entity types with the registry
 *     UniversalRepositoryRegistry registry = ctx.get(UniversalRepositoryRegistry.class);
 *
 *     // Register a single entity
 *     registry.register(User.class, UUID.class);
 *
 *     // Register with custom configuration
 *     registry.register(Order.class, Long.class, builder ->
 *         builder.withOptimizations(Optimizations.AGGRESSIVE_SETTINGS)
 *     );
 *
 *     // Get repository adapter
 *     RepositoryAdapter<User, UUID> userRepo = registry.get(User.class, UUID.class);
 * });
 * }</pre>
 *
 * @see UniversalRepositoryRegistry
 */
public class UniversalDataSourceAware implements DataSourceAware {

    private volatile DataSource dataSource;
    private volatile JdbcConfig jdbcConfig;
    private final Map<String, Object> extensionConfig = new ConcurrentHashMap<>();
    private volatile UniversalRepositoryRegistry registry;

    @Override
    public String name() {
        return "universal";
    }

    @Override
    public void acceptDataSource(DataSource dataSource, JdbcConfig config) {
        this.dataSource = dataSource;
        this.jdbcConfig = config;
        this.extensionConfig.putAll(config.extensionConfig("universal"));
    }

    @Override
    public void configure(MagnesiumRuntime runtime) {
        Objects.requireNonNull(dataSource, "DataSource not available");

        this.registry = new UniversalRepositoryRegistry(
            dataSource,
            getDialect(),
            Optimizations.RECOMMENDED_SETTINGS,
            null // Service registration happens below
        );

        runtime.services(services -> {
            services.registerInstance(UniversalRepositoryRegistry.class, registry);
            services.registerInstance(UniversalDataSourceAware.class, this);
            // Update registry with service registrar for adapter registration
            registry.setServiceRegistrar(services);
        });
    }

    /**
     * Gets the registry. Use this to register entities and obtain adapters.
     *
     * @return the repository registry
     * @throws IllegalStateException if not yet configured
     */
    public @NotNull UniversalRepositoryRegistry getRegistry() {
        if (registry == null) {
            throw new IllegalStateException("Registry not initialized. Ensure configure() was called.");
        }
        return registry;
    }

    private SQLQueryValidator.SQLDialect getDialect() {
        String dialectName = (String) extensionConfig.get("dialect");
        if (dialectName != null) {
            return SQLQueryValidator.SQLDialect.valueOf(dialectName.toUpperCase());
        }

        String url = jdbcConfig.url();
        if (url != null) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains("mysql")) {
                return SQLQueryValidator.SQLDialect.MYSQL;
            } else if (lowerUrl.contains("postgresql") || lowerUrl.contains("postgres")) {
                return SQLQueryValidator.SQLDialect.POSTGRESQL;
            } else if (lowerUrl.contains("sqlite")) {
                return SQLQueryValidator.SQLDialect.SQLITE;
            }
        }

        return SQLQueryValidator.SQLDialect.POSTGRESQL;
    }
}
