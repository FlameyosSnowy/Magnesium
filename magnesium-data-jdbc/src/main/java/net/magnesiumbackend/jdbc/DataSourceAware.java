package net.magnesiumbackend.jdbc;

import net.magnesiumbackend.core.MagnesiumRuntime;

import javax.sql.DataSource;

/**
 * Interface for extensions that need to be initialized with a DataSource.
 *
 * <p>Extensions implementing this interface will be automatically discovered
 * and called when the DataSource is available. This allows multiple ORMs,
 * caching layers, and other database-related extensions to be auto-wired.</p>
 *
 * <p>Implementations must be registered as services in module-info:</p>
 * <pre>{@code
 * provides net.magnesiumbackend.jdbc.DataSourceAware
 *     with my.extension.MyExtension;
 * }</pre>
 *
 * <h3>Example implementation</h3>
 * <pre>{@code
 * public class HibernateExtension implements DataSourceAware {
 *     @Override
 *     public String name() {
 *         return "hibernate";
 *     }
 *
 *     @Override
 *     public void acceptDataSource(DataSource dataSource, JdbcConfig config) {
 *         Map<String, Object> hibernateProps = config.extensionConfig("hibernate");
 *
 *         StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
 *         builder.applySetting("hibernate.connection.datasource", dataSource);
 *         hibernateProps.forEach(builder::applySetting);
 *
 *         this.sessionFactory = new MetadataSources(builder.build())
 *             .addAnnotatedClasses(getEntities())
 *             .buildMetadata()
 *             .buildSessionFactory();
 *     }
 *
 *     @Override
 *     public void configure(MagnesiumRuntime runtime) {
 *         // Register SessionFactory as a service
 *         runtime.services(s -> s.registerInstance(SessionFactory.class, sessionFactory));
 *     }
 * }
 * }</pre>
 *
 * <h3>Extension Configuration</h3>
 * <p>Extensions can access their configuration via {@link JdbcConfig#extensionConfig(String)}.
 * The extension name in the config file should match the value returned by {@link #name()}.</p>
 *
 * <pre>{@code
 * [jdbc.extensions.hibernate]
 * ddl-auto = "update"
 * show-sql = true
 * format-sql = true
 * use-sql-comments = true
 *
 * [jdbc.extensions.hibernate.cache]
 * use-second-level-cache = true
 * region.factory-class = "org.hibernate.cache.jcache.JCacheRegionFactory"
 * default-cache-region-strategy = "read-write"
 * }</pre>
 */
public interface DataSourceAware {

    /**
     * Returns the unique name of this extension.
     *
     * <p>This name is used to:
     * <ul>
     *   <li>Load extension-specific configuration from {@code [jdbc.extensions.<name>]}</li>
     *   <li>Identify the extension in logs and debugging</li>
     * </ul></p>
     *
     * @return the extension name (e.g., "hibernate", "jooq", "mybatis")
     */
    String name();

    /**
     * Called when the DataSource is available.
     *
     * <p>This method is called during application startup, before
     * {@link #configure(MagnesiumRuntime)}.</p>
     *
     * @param dataSource the configured DataSource with connection pooling
     * @param config the JDBC configuration including extension settings
     */
    void acceptDataSource(javax.sql.DataSource dataSource, JdbcConfig config);

    /**
     * Configures the Magnesium runtime with services provided by this extension.
     *
     * <p>Called after {@link #acceptDataSource(DataSource, JdbcConfig)}.</p>
     *
     * @param runtime the Magnesium runtime
     */
    void configure(net.magnesiumbackend.core.MagnesiumRuntime runtime);
}
