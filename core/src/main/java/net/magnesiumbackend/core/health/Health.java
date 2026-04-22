package net.magnesiumbackend.core.health;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the health status of a component or the entire system.
 *
 * <p>Health status follows Spring Boot conventions:</p>
 * <ul>
 *   <li><strong>UP</strong> - Component is functioning normally</li>
 *   <li><strong>DOWN</strong> - Component has failed</li>
 *   <li><strong>UNKNOWN</strong> - Status cannot be determined</li>
 *   <li><strong>OUT_OF_SERVICE</strong> - Component is intentionally offline</li>
 * </ul>
 *
 * @see HealthIndicator
 * @see HealthStatus
 */
public final class Health {

    private final HealthStatus status;
    private final Map<String, Object> details;

    private Health(Builder builder) {
        this.status = builder.status;
        this.details = builder.details.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(builder.details));
    }

    /**
     * Creates a health instance with UP status.
     *
     * @return builder for UP status
     */
    public static Builder up() {
        return new Builder(HealthStatus.UP);
    }

    /**
     * Creates a health instance with DOWN status.
     *
     * @return builder for DOWN status
     */
    public static Builder down() {
        return new Builder(HealthStatus.DOWN);
    }

    /**
     * Creates a health instance with UNKNOWN status.
     *
     * @return builder for UNKNOWN status
     */
    public static Builder unknown() {
        return new Builder(HealthStatus.UNKNOWN);
    }

    /**
     * Creates a health instance with OUT_OF_SERVICE status.
     *
     * @return builder for OUT_OF_SERVICE status
     */
    public static Builder outOfService() {
        return new Builder(HealthStatus.OUT_OF_SERVICE);
    }

    /**
     * Creates a health instance for the given status.
     *
     * @param status the status
     * @return builder for the given status
     */
    public static Builder status(@NotNull HealthStatus status) {
        return new Builder(Objects.requireNonNull(status, "status"));
    }

    /**
     * Returns the health status.
     *
     * @return the status
     */
    public @NotNull HealthStatus status() {
        return status;
    }

    /**
     * Returns additional details about the health status.
     *
     * @return unmodifiable map of details
     */
    public @NotNull Map<String, Object> details() {
        return details;
    }

    /**
     * Returns whether the status is UP.
     *
     * @return true if status is UP
     */
    public boolean isUp() {
        return status == HealthStatus.UP;
    }

    /**
     * Returns whether the status is DOWN.
     *
     * @return true if status is DOWN
     */
    public boolean isDown() {
        return status == HealthStatus.DOWN;
    }

    @Override
    public String toString() {
        return "Health{status=" + status + ", details=" + details + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Health health)) return false;
        return status == health.status && details.equals(health.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, details);
    }

    /**
     * Builder for creating Health instances.
     */
    public static final class Builder {
        private final HealthStatus status;
        private final Map<String, Object> details = new LinkedHashMap<>();

        private Builder(HealthStatus status) {
            this.status = status;
        }

        /**
         * Adds a detail entry.
         *
         * @param key the detail key
         * @param value the detail value
         * @return this builder
         */
        public Builder withDetail(@NotNull String key, @Nullable Object value) {
            details.put(Objects.requireNonNull(key, "key"), value);
            return this;
        }

        /**
         * Adds exception details.
         *
         * @param ex the exception
         * @return this builder
         */
        public Builder withException(@NotNull Throwable ex) {
            details.put("error", ex.getMessage());
            details.put("exception", ex.getClass().getSimpleName());
            return this;
        }

        /**
         * Builds the Health instance.
         *
         * @return the health instance
         */
        public Health build() {
            return new Health(this);
        }
    }
}
