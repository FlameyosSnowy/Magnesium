package net.magnesiumbackend.jdbc;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Provider interface for accessing the configured DataSource.
 *
 * <p>Extensions (like Hibernate, JOOQ, etc.) can depend on this provider
 * to obtain the DataSource managed by the JDBC module.</p>
 *
 * <p>Example usage in an extension:</p>
 * <pre>{@code
 * public class HibernateExtension implements MagnesiumExtension {
 *     @Override
 *     public void configure(MagnesiumRuntime runtime) {
 *         DataSourceProvider provider = runtime.serviceRegistry()
 *             .get(DataSourceProvider.class);
 *
 *         DataSource dataSource = provider.getDataSource()
 *             .orElseThrow(() -> new IllegalStateException("No DataSource configured"));
 *
 *         // Configure Hibernate with the DataSource
 *         JdbcConfig config = runtime.configurationManager().get(JdbcConfig.class);
 *         Map<String, Object> hibernateProps = config.extensionConfig("hibernate");
 *
 *         SessionFactory sessionFactory = buildSessionFactory(dataSource, hibernateProps);
 *         runtime.services(s -> s.registerInstance(SessionFactory.class, sessionFactory));
 *     }
 * }
 * }</pre>
 */
public interface DataSourceProvider extends Supplier<Optional<DataSource>> {

    /**
     * Gets the configured DataSource if available.
     *
     * @return the DataSource, or empty if JDBC is not configured
     */
    Optional<DataSource> getDataSource();

    /**
     * Gets the JdbcConfig used to create the DataSource.
     *
     * @return the configuration
     */
    JdbcConfig getConfig();

    @Override
    default Optional<DataSource> get() {
        return getDataSource();
    }
}
