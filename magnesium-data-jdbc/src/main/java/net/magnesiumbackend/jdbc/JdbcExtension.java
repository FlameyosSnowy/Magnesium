package net.magnesiumbackend.jdbc;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.extensions.MagnesiumExtension;
import net.magnesiumbackend.jdbc.internal.DataSourceProviderImpl;
import net.magnesiumbackend.jdbc.internal.InternalJdbcClient;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * JDBC extension for Magnesium framework.
 *
 * <p>Auto-configures JDBC support with connection pooling (HikariCP) when
 * magnesium-data-jdbc is on the classpath. Also auto-wires {@link DataSourceAware}
 * extensions like Hibernate, JOOQ, etc.</p>
 *
 * <h3>Configuration (application.toml)</h3>
 * <pre>{@code
 * [jdbc]
 * url = "jdbc:postgresql://localhost:5432/mydb"
 * username = "postgres"
 * password = "secret"
 *
 * [jdbc.pool]
 * maximum-pool-size = 20
 * minimum-idle = 5
 * }</pre>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Get JdbcClient from service registry
 * JdbcClient jdbc = runtime.serviceRegistry().get(JdbcClient.class);
 *
 * // Or get DataSource for other libraries
 * DataSource ds = runtime.serviceRegistry()
 *     .get(DataSourceProvider.class)
 *     .getDataSource()
 *     .orElseThrow();
 * }</pre>
 */
public class JdbcExtension implements MagnesiumExtension {

    @Override
    public String name() {
        return "jdbc";
    }

    @Override
    public void configure(@NotNull MagnesiumRuntime runtime) {
        JdbcConfig config = loadConfig(runtime);

        DataSourceProviderImpl provider = new DataSourceProviderImpl(config);

        runtime.services(services -> {
            services.registerInstance(DataSourceProvider.class, provider);

            provider.getDataSource().ifPresent(ds -> {
                services.registerInstance(DataSource.class, ds);
                services.register(JdbcClient.class, ctx -> new InternalJdbcClient(ds));
            });
        });

        wireDataSourceAwareExtensions(runtime, provider);
    }

    private JdbcConfig loadConfig(MagnesiumRuntime runtime) {
        try {
            return runtime.configurationManager().get(JdbcConfig.class);
        } catch (IllegalStateException e) {
            return JdbcConfig.builder().build();
        }
    }

    private void wireDataSourceAwareExtensions(MagnesiumRuntime runtime, DataSourceProviderImpl provider) {
        List<DataSourceAware> extensions = new ArrayList<>(2);
        for (DataSourceAware dataSourceAware : ServiceLoader.load(DataSourceAware.class)) {
            extensions.add(dataSourceAware);
        }

        extensions.sort(Comparator.comparing(DataSourceAware::name));

        provider.getDataSource().ifPresent(ds -> {
            for (DataSourceAware extension : extensions) {
                extension.acceptDataSource(ds, provider.getConfig());
            }
        });

        for (DataSourceAware extension : extensions) {
            extension.configure(runtime);
        }
    }
}
