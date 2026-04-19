package net.magnesiumbackend.examples.annotations;

import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.annotations.ApplicationProperties;
import net.magnesiumbackend.core.annotations.ApplicationProperty;
import net.magnesiumbackend.core.annotations.RestController;
import net.magnesiumbackend.core.annotations.GetMapping;
import net.magnesiumbackend.core.http.response.ResponseEntity;

import java.time.Duration;
import java.util.List;

/// Example demonstrating @ApplicationProperties with TOML configuration.
///
/// This shows how to use type-safe properties with compile-time validation,
/// TOML configuration files, and hot-reload support.
///
/// Example application.toml:
///
/// ```toml
/// [server]
/// host = "0.0.0.0"
/// port = 8080
/// timeout = "30s"
///
/// [database]
/// url = "jdbc:postgresql://localhost:5432/mydb"
/// pool-size = 10
/// enabled = true
/// ```
///
/// Hot-reload: Modify application.toml while the app is running, changes
/// are picked up immediately without restart!
public class PropertiesExample extends Application {

    /**
     * Server configuration as a record - immutable and concise.
     */
    @ApplicationProperties(prefix = "server")
    public record ServerProperties(
        @ApplicationProperty(name = "host", defaultValue = "0.0.0.0") String host,
        @ApplicationProperty(name = "port", defaultValue = "8080", min = 1, max = 65535) int port,
        @ApplicationProperty(name = "timeout", defaultValue = "30s") Duration timeout
    ) {}

    /**
     * Database configuration - class with validation.
     */
    @ApplicationProperties(prefix = "database")
    public static class DatabaseProperties {

        @ApplicationProperty(name = "url", required = true)
        private String url;

        @ApplicationProperty(name = "pool-size", defaultValue = "10", min = 1, max = 100)
        private int poolSize;

        @ApplicationProperty(name = "enabled", defaultValue = "true")
        private boolean enabled;

        @ApplicationProperty(name = "features")
        private List<String> features;

        // Getters
        public String getUrl() { return url; }
        public int getPoolSize() { return poolSize; }
        public boolean isEnabled() { return enabled; }
        public List<String> getFeatures() { return features != null ? features : List.of(); }
    }

    /**
     * Feature flags - record with defaults.
     */
    @ApplicationProperties(prefix = "features")
    public record FeatureFlags(
        @ApplicationProperty(defaultValue = "true") boolean caching,
        @ApplicationProperty(defaultValue = "false") boolean rateLimiting,
        @ApplicationProperty(defaultValue = "true") boolean metrics
    ) {}

    @RestController
    public static class ConfigController {
        private final ServerProperties serverProps;
        private final DatabaseProperties dbProps;
        private final FeatureFlags features;

        public ConfigController(ServerProperties serverProps, DatabaseProperties dbProps, FeatureFlags features) {
            this.serverProps = serverProps;
            this.dbProps = dbProps;
            this.features = features;
        }

        @GetMapping(path = "/api/config")
        public ResponseEntity<ConfigResponse> getConfig() {
            return ResponseEntity.ok(new ConfigResponse(
                serverProps.getHost() + ":" + serverProps.getPort(),
                dbProps.getUrl(),
                features.isCaching(),
                features.isRateLimiting()
            ));
        }
    }

    public record ConfigResponse(
        String server,
        String databaseUrl,
        boolean cachingEnabled,
        boolean rateLimitingEnabled
    ) {}

    @Override
    public void configure(net.magnesiumbackend.core.MagnesiumRuntime runtime) {
        // Properties are auto-loaded via MagnesiumConfigurationManager
        // Just need to register the property classes
        runtime.configuration(cfg -> cfg
            .toml(java.nio.file.Path.of("application.toml"))
            .env()
        );
    }

    public static void main(String[] args) {
        MagnesiumApplication.run(new PropertiesExample(), 8080);
    }
}
