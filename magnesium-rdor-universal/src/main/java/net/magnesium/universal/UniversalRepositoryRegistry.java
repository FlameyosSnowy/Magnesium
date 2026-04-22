package net.magnesium.universal;

import io.github.flameyossnowy.universal.api.Optimizations;
import io.github.flameyossnowy.universal.api.RepositoryAdapter;
import io.github.flameyossnowy.universal.mysql.MySQLRepositoryAdapter;
import io.github.flameyossnowy.universal.mysql.MySQLRepositoryAdapterBuilder;
import io.github.flameyossnowy.universal.postgresql.PostgreSQLRepositoryAdapter;
import io.github.flameyossnowy.universal.postgresql.PostgreSQLRepositoryAdapterBuilder;
import io.github.flameyossnowy.universal.sql.internals.SQLConnectionProvider;
import io.github.flameyossnowy.universal.sql.query.SQLQueryValidator;
import io.github.flameyossnowy.universal.sqlite.SQLiteRepositoryAdapter;
import io.github.flameyossnowy.universal.sqlite.SQLiteRepositoryAdapterBuilder;
import net.magnesiumbackend.core.services.ServiceRegistrar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Registry for managing multiple Universal ORM RepositoryAdapters.
 *
 * <p>This registry provides a central way to create, cache, and access
 * repository adapters for multiple entity types using a shared DataSource.
 * It supports custom configuration per entity type and thread-safe access.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Get the registry from Magnesium service context
 * UniversalRepositoryRegistry registry = ctx.get(UniversalRepositoryRegistry.class);
 *
 * // Register entity types (typically done in Application.configure())
 * registry.register(User.class, UUID.class);
 * registry.register(Order.class, Long.class);
 * registry.register(Product.class, Integer.class);
 *
 * // Use repositories in your services
 * RepositoryAdapter<User, UUID> userRepo = registry.get(User.class, UUID.class);
 * Optional<User> user = userRepo.findById(userId);
 *
 * // Register with custom configuration
 * registry.register(Analytics.class, Long.class, builder ->
 *     builder.withOptimizations(Optimizations.AGGRESSIVE_SETTINGS)
 * );
 *
 * // Batch register multiple entities
 * registry.registerAll(Map.of(
 *     User.class, UUID.class,
 *     Order.class, Long.class,
 *     Product.class, Integer.class
 * ));
 * }</pre>
 *
 * @see UniversalDataSourceAware
 */
@SuppressWarnings("unused")
public final class UniversalRepositoryRegistry {

    private final DataSource dataSource;
    private final SQLQueryValidator.SQLDialect dialect;
    private final Optimizations defaultOptimizations;
    private volatile ServiceRegistrar serviceRegistrar;

    // Cache of created adapters
    private final Map<EntityKey<?, ?>, RepositoryAdapter<?, ?, Connection>> adapters = new ConcurrentHashMap<>();

    // Custom configurations per entity type
    private final Map<EntityKey<?, ?>, Function<?, ?>> customizers = new ConcurrentHashMap<>();

    UniversalRepositoryRegistry(
            @NotNull DataSource dataSource,
            @NotNull SQLQueryValidator.SQLDialect dialect,
            @NotNull Optimizations defaultOptimizations,
            @Nullable ServiceRegistrar serviceRegistrar) {
        this.dataSource = Objects.requireNonNull(dataSource, "DataSource cannot be null");
        this.dialect = Objects.requireNonNull(dialect, "SQLDialect cannot be null");
        this.defaultOptimizations = Objects.requireNonNull(defaultOptimizations, "Optimizations cannot be null");
        this.serviceRegistrar = serviceRegistrar;
    }

    /**
     * Sets the service registrar for adapter registration.
     * Called by UniversalDataSourceAware during configuration.
     */
    void setServiceRegistrar(@Nullable ServiceRegistrar registrar) {
        this.serviceRegistrar = registrar;
    }

    /**
     * Registers an entity type for repository access.
     *
     * <p>The repository adapter is created lazily on first access and cached.
     * Subsequent calls with the same entity and ID class return the same adapter.</p>
     *
     * @param entityClass the entity class
     * @param idClass the ID class
     * @param <T> entity type
     * @param <ID> ID type
     * @return this registry for method chaining
     */
    public <T, ID> UniversalRepositoryRegistry register(
            @NotNull Class<T> entityClass,
            @NotNull Class<ID> idClass) {
        return register(entityClass, idClass, null);
    }

    /**
     * Registers an entity type with custom builder configuration.
     *
     * <p>The customizer function receives the appropriate builder type based on
     * the configured dialect (MySQL, PostgreSQL, or SQLite).</p>
     *
     * @param entityClass the entity class
     * @param idClass the ID class
     * @param customizer optional builder customizer function
     * @param <T> entity type
     * @param <ID> ID type
     * @return this registry for method chaining
     */
    public <T, ID> UniversalRepositoryRegistry register(
            @NotNull Class<T> entityClass,
            @NotNull Class<ID> idClass,
            @Nullable Function<?, ?> customizer) {

        EntityKey<T, ID> key = new EntityKey<>(entityClass, idClass);

        // Store customizer for lazy adapter creation
        if (customizer != null) {
            customizers.put(key, customizer);
        }

        registerIfAbsent(entityClass, idClass);

        return this;
    }

    /**
     * Batch registers multiple entity types.
     *
     * @param entities map of entity classes to their ID classes
     * @return this registry for method chaining
     */
    @SuppressWarnings("unchecked")
    public UniversalRepositoryRegistry registerAll(
            @NotNull Map<Class<?>, Class<?>> entities) {
        entities.forEach((entityClass, idClass) ->
            register(
                (Class<Object>) entityClass,
                (Class<Object>) idClass
            )
        );
        return this;
    }

    /**
     * Gets or creates a RepositoryAdapter for the given entity type.
     *
     * <p>This method is thread-safe. Adapters are created once and cached.</p>
     *
     * @param entityClass the entity class
     * @param idClass the ID class
     * @param <T> entity type
     * @param <ID> ID type
     * @return the repository adapter
     * @throws IllegalArgumentException if the entity type is not registered
     */
    public <T, ID> RepositoryAdapter<T, ID, Connection> get(
            @NotNull Class<T> entityClass,
            @NotNull Class<ID> idClass) {
        return registerIfAbsent(entityClass, idClass);
    }

    @SuppressWarnings("unchecked")
    private <T, ID> RepositoryAdapter<T, ID, Connection> registerIfAbsent(@NotNull Class<T> entityClass, @NotNull Class<ID> idClass) {
        EntityKey<T, ID> key = new EntityKey<>(entityClass, idClass);

        RepositoryAdapter<T, ID, Connection> adapter = (RepositoryAdapter<T, ID, Connection>) adapters.computeIfAbsent(key, _ -> {
            SQLConnectionProvider connectionProvider = new DataSourceConnectionProvider(dataSource);
            Function<?, ?> customizer = customizers.get(key);
            return createAdapter(entityClass, idClass, connectionProvider, customizer);
        });

        // Register to ServiceRegistry if registrar available (type-erased due to generics)
        if (serviceRegistrar != null) {
            serviceRegistrar.registerInstance(RepositoryAdapter.class, adapter);
        }
        return adapter;
    }

    /**
     * Alias for {@link #get(Class, Class)} with a more descriptive name.
     *
     * @param entityClass the entity class
     * @param idClass the ID class
     * @param <T> entity type
     * @param <ID> ID type
     * @return the repository adapter
     */
    public <T, ID> RepositoryAdapter<T, ID, Connection> getAdapter(
            @NotNull Class<T> entityClass,
            @NotNull Class<ID> idClass) {
        return get(entityClass, idClass);
    }

    /**
     * Gets or creates a RepositoryAdapter with custom builder configuration.
     *
     * <p>If the adapter was already created without a customizer, the cached
     * adapter is returned. To ensure custom configuration, call this method
     * before the standard {@link #get(Class, Class)}.</p>
     *
     * @param entityClass the entity class
     * @param idClass the ID class
     * @param customizer builder customizer function
     * @param <T> entity type
     * @param <ID> ID type
     * @return the repository adapter
     */
    public <T, ID> RepositoryAdapter<T, ID, Connection> getAdapter(
            @NotNull Class<T> entityClass,
            @NotNull Class<ID> idClass,
            @NotNull Function<?, ?> customizer) {

        EntityKey<T, ID> key = new EntityKey<>(entityClass, idClass);

        // Store customizer if adapter not yet created
        customizers.putIfAbsent(key, customizer);

        return get(entityClass, idClass);
    }

    /**
     * Checks if an adapter exists for the given entity type.
     *
     * @param entityClass the entity class
     * @param idClass the ID class
     * @return true if an adapter has been created
     */
    public boolean hasAdapter(@NotNull Class<?> entityClass, @NotNull Class<?> idClass) {
        return adapters.containsKey(new EntityKey<>(entityClass, idClass));
    }

    /**
     * Gets all registered entity keys.
     *
     * @return unmodifiable set of registered entity keys
     */
    public Set<EntityKey<?, ?>> getRegisteredEntities() {
        return Collections.unmodifiableSet(adapters.keySet());
    }

    /**
     * Closes all managed repository adapters.
     *
     * <p>This should be called during application shutdown to properly
     * release resources.</p>
     */
    public void closeAll() {
        adapters.values().forEach(adapter -> {
            try {
                adapter.close();
            } catch (Exception e) {
                // Log but continue closing others
                System.err.println("Error closing repository adapter: " + e.getMessage());
            }
        });
        adapters.clear();
        customizers.clear();
    }

    // Getter methods for configuration access

    public @NotNull DataSource getDataSource() {
        return dataSource;
    }

    public @NotNull SQLQueryValidator.SQLDialect getDialect() {
        return dialect;
    }

    public @NotNull Optimizations getDefaultOptimizations() {
        return defaultOptimizations;
    }

    private <T, ID> RepositoryAdapter<T, ID, Connection> createAdapter(
            Class<T> entityClass,
            Class<ID> idClass,
            SQLConnectionProvider connectionProvider,
            Function<?, ?> customizer) {

        return switch (dialect) {
            case MYSQL -> createMySQLAdapter(entityClass, idClass, connectionProvider, customizer);
            case POSTGRESQL -> createPostgreSQLAdapter(entityClass, idClass, connectionProvider, customizer);
            case SQLITE -> createSQLiteAdapter(entityClass, idClass, connectionProvider, customizer);
            default -> throw new UnsupportedOperationException("Unsupported dialect: " + dialect);
        };
    }

    @SuppressWarnings({"unchecked" })
    private <T, ID> RepositoryAdapter<T, ID, Connection> createMySQLAdapter(
            Class<T> entityClass,
            Class<ID> idClass,
            SQLConnectionProvider connectionProvider,
            Function<?, ?> customizer) {

        MySQLRepositoryAdapterBuilder<T, ID> builder = MySQLRepositoryAdapter
            .builder(entityClass, idClass)
            .withConnectionProvider((_, _) -> connectionProvider)
            .withOptimizations(defaultOptimizations);

        if (customizer != null) {
            builder = ((Function<MySQLRepositoryAdapterBuilder<T, ID>,
                MySQLRepositoryAdapterBuilder<T, ID>>) customizer).apply(builder);
        }
        return builder.build();
    }

    @SuppressWarnings({"unchecked" })
    private <T, ID> RepositoryAdapter<T, ID, Connection> createPostgreSQLAdapter(
            Class<T> entityClass,
            Class<ID> idClass,
            SQLConnectionProvider connectionProvider,
            Function<?, ?> customizer) {

        PostgreSQLRepositoryAdapterBuilder<T, ID> builder = PostgreSQLRepositoryAdapter
            .builder(entityClass, idClass)
            .withConnectionProvider((_, _) -> connectionProvider)
            .withOptimizations(defaultOptimizations);

        if (customizer != null) {
            builder = ((Function<PostgreSQLRepositoryAdapterBuilder<T, ID>,
                PostgreSQLRepositoryAdapterBuilder<T, ID>>) customizer).apply(builder);
        }
        return builder.build();
    }

    @SuppressWarnings({"unchecked" })
    private <T, ID> RepositoryAdapter<T, ID, Connection> createSQLiteAdapter(
            Class<T> entityClass,
            Class<ID> idClass,
            SQLConnectionProvider connectionProvider,
            Function<?, ?> customizer) {

        SQLiteRepositoryAdapterBuilder<T, ID> builder = SQLiteRepositoryAdapter
            .builder(entityClass, idClass)
            .withConnectionProvider((_, _) -> connectionProvider)
            .withOptimizations(defaultOptimizations);

        if (customizer != null) {
            builder = ((Function<SQLiteRepositoryAdapterBuilder<T, ID>,
                SQLiteRepositoryAdapterBuilder<T, ID>>) customizer).apply(builder);
        }
        return builder.build();
    }

    /**
     * Composite key for entity type registration.
     *
     * @param entityClass the entity class
     * @param idClass the ID class
     */
    public record EntityKey<T, ID>(@NotNull Class<T> entityClass, @NotNull Class<ID> idClass) {
        public EntityKey {
            Objects.requireNonNull(entityClass, "entityClass cannot be null");
            Objects.requireNonNull(idClass, "idClass cannot be null");
        }
    }
}
