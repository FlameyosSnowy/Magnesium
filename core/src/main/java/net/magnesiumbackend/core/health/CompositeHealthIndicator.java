package net.magnesiumbackend.core.health;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Aggregates multiple health indicators into a single composite health check.
 *
 * <p>The composite health is calculated as follows:</p>
 * <ul>
 *   <li>Status is DOWN if any component is DOWN</li>
 *   <li>Status is OUT_OF_SERVICE if any component is OUT_OF_SERVICE (and none are DOWN)</li>
 *   <li>Status is UNKNOWN if any component is UNKNOWN (and none are more severe)</li>
 *   <li>Status is UP only if all components are UP</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * CompositeHealthIndicator composite = new CompositeHealthIndicator();
 * composite.add(new DatabaseHealthIndicator(pool));
 * composite.add(new RedisHealthIndicator(redisService));
 *
 * Health health = composite.health();
 * }</pre>
 */
public final class CompositeHealthIndicator implements HealthIndicator {

    private final Map<String, HealthIndicator> indicators = new ConcurrentHashMap<>();
    private final Executor executor;
    private final Duration timeout;

    /**
     * Creates a composite indicator with virtual thread executor.
     */
    public CompositeHealthIndicator() {
        this(Executors.newVirtualThreadPerTaskExecutor(), Duration.ofSeconds(5));
    }

    /**
     * Creates a composite indicator with custom executor and timeout.
     *
     * @param executor executor for parallel health checks
     * @param timeout timeout for each health check
     */
    public CompositeHealthIndicator(@NotNull Executor executor, @NotNull Duration timeout) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    /**
     * Adds a health indicator.
     *
     * @param indicator the indicator to add
     * @return this composite indicator
     */
    public CompositeHealthIndicator add(@NotNull HealthIndicator indicator) {
        Objects.requireNonNull(indicator, "indicator");
        indicators.put(indicator.name(), indicator);
        return this;
    }

    /**
     * Adds a health indicator with a custom name.
     *
     * @param name the name for this indicator
     * @param indicator the indicator to add
     * @return this composite indicator
     */
    public CompositeHealthIndicator add(@NotNull String name, @NotNull HealthIndicator indicator) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(indicator, "indicator");
        indicators.put(name, indicator);
        return this;
    }

    /**
     * Removes a health indicator.
     *
     * @param name the name of the indicator to remove
     * @return this composite indicator
     */
    public CompositeHealthIndicator remove(@NotNull String name) {
        indicators.remove(Objects.requireNonNull(name, "name"));
        return this;
    }

    @Override
    public @NotNull Health health() {
        if (indicators.isEmpty()) {
            return Health.up().build();
        }

        // Run health checks in parallel
        Map<String, Future<Health>> futures = new HashMap<>();

        for (Map.Entry<String, HealthIndicator> entry : indicators.entrySet()) {
            futures.put(entry.getKey(), CompletableFuture.supplyAsync(
                () -> checkSafely(entry.getValue()),
                executor
            ));
        }

        // Collect results
        HealthStatus aggregateStatus = HealthStatus.UP;
        Map<String, Health> componentResults = new LinkedHashMap<>();

        for (Map.Entry<String, Future<Health>> entry : futures.entrySet()) {
            String name = entry.getKey();
            Health health;

            try {
                health = entry.getValue().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                health = Health.down()
                    .withDetail("error", "Health check timed out after " + timeout)
                    .build();
            } catch (Exception e) {
                health = Health.down()
                    .withException(e)
                    .build();
            }

            componentResults.put(name, health);
            aggregateStatus = aggregateStatus.combine(health.status());
        }

        // Build composite health
        Health.Builder builder = Health.status(aggregateStatus);

        for (Map.Entry<String, Health> entry : componentResults.entrySet()) {
            builder.withDetail(entry.getKey(), Map.of(
                "status", entry.getValue().status().code(),
                "details", entry.getValue().details()
            ));
        }

        return builder.build();
    }

    private Health checkSafely(HealthIndicator indicator) {
        try {
            return indicator.health();
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }

    @Override
    public @NotNull String name() {
        return "composite";
    }

    /**
     * Returns the registered indicators.
     *
     * @return unmodifiable map of indicators
     */
    public @NotNull Map<String, HealthIndicator> indicators() {
        return Collections.unmodifiableMap(indicators);
    }
}
