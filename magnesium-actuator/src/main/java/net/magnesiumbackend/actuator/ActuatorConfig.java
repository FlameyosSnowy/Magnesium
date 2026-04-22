package net.magnesiumbackend.actuator;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for the actuator extension.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * ActuatorConfig config = ActuatorConfig.builder()
 *     .basePath("/actuator")
 *     .enableMetrics(true)
 *     .enableHealth(true)
 *     .healthPath("/health")
 *     .build();
 * }</pre>
 */
public final class ActuatorConfig {

    private final String basePath;
    private final String healthPath;
    private final String metricsPath;
    private final boolean enableHealth;
    private final boolean enableMetrics;
    private final boolean enableJvmHealth;
    private final boolean enableDiskHealth;
    private final Set<String> healthPathExclusions;

    private ActuatorConfig(Builder builder) {
        this.basePath = builder.basePath;
        this.healthPath = builder.healthPath;
        this.metricsPath = builder.metricsPath;
        this.enableHealth = builder.enableHealth;
        this.enableMetrics = builder.enableMetrics;
        this.enableJvmHealth = builder.enableJvmHealth;
        this.enableDiskHealth = builder.enableDiskHealth;
        this.healthPathExclusions = Set.copyOf(builder.healthPathExclusions);
    }

    /**
     * Creates a builder with default settings.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
     }

    /**
     * Creates a configuration with defaults.
     *
     * @return default configuration
     */
    public static ActuatorConfig defaults() {
        return new Builder().build();
    }

    public @NotNull String basePath() {
        return basePath;
    }

    public @NotNull String healthPath() {
        return healthPath;
    }

    public @NotNull String metricsPath() {
        return metricsPath;
    }

    public boolean enableHealth() {
        return enableHealth;
    }

    public boolean enableMetrics() {
        return enableMetrics;
    }

    public boolean enableJvmHealth() {
        return enableJvmHealth;
    }

    public boolean enableDiskHealth() {
        return enableDiskHealth;
    }

    public @NotNull Set<String> healthPathExclusions() {
        return healthPathExclusions;
    }

    /**
     * Builder for ActuatorConfig.
     */
    public static final class Builder {
        private String basePath = "/actuator";
        private String healthPath = "/health";
        private String metricsPath = "/metrics";
        private boolean enableHealth = true;
        private boolean enableMetrics = true;
        private boolean enableJvmHealth = true;
        private boolean enableDiskHealth = true;
        private Set<String> healthPathExclusions = new HashSet<>();

        public Builder basePath(@NotNull String path) {
            this.basePath = Objects.requireNonNull(path, "basePath");
            return this;
        }

        public Builder healthPath(@NotNull String path) {
            this.healthPath = Objects.requireNonNull(path, "healthPath");
            return this;
        }

        public Builder metricsPath(@NotNull String path) {
            this.metricsPath = Objects.requireNonNull(path, "metricsPath");
            return this;
        }

        public Builder enableHealth(boolean enable) {
            this.enableHealth = enable;
            return this;
        }

        public Builder enableMetrics(boolean enable) {
            this.enableMetrics = enable;
            return this;
        }

        public Builder enableJvmHealth(boolean enable) {
            this.enableJvmHealth = enable;
            return this;
        }

        public Builder enableDiskHealth(boolean enable) {
            this.enableDiskHealth = enable;
            return this;
        }

        public Builder excludeHealthPath(@NotNull String path) {
            this.healthPathExclusions.add(Objects.requireNonNull(path, "path"));
            return this;
        }

        public ActuatorConfig build() {
            return new ActuatorConfig(this);
        }
    }
}
