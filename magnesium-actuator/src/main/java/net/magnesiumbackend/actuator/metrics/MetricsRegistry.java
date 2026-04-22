package net.magnesiumbackend.actuator.metrics;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Registry for application metrics.
 *
 * <p>Provides counters, gauges, and timers for monitoring application health.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * MetricsRegistry metrics = new MetricsRegistry();
 *
 * // Counter
 * Counter requests = metrics.counter("http.requests");
 * requests.increment();
 *
 * // Gauge
 * metrics.gauge("memory.used", () -> Runtime.getRuntime().totalMemory());
 *
 * // Timer
 * Timer timer = metrics.timer("db.query");
 * timer.record(() -> database.query());
 * }</pre>
 */
public final class MetricsRegistry {

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Gauge> gauges = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final String prefix;

    /**
     * Creates a metrics registry with no prefix.
     */
    public MetricsRegistry() {
        this("");
    }

    /**
     * Creates a metrics registry with the given prefix.
     *
     * @param prefix metric name prefix (e.g., "app.")
     */
    public MetricsRegistry(@NotNull String prefix) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    /**
     * Gets or creates a counter.
     *
     * @param name the counter name
     * @return the counter
     */
    public @NotNull Counter counter(@NotNull String name) {
        String fullName = prefix + name;
        return counters.computeIfAbsent(fullName, Counter::new);
    }

    /**
     * Registers a gauge that returns a long value.
     *
     * @param name the gauge name
     * @param supplier the value supplier
     * @return this registry
     */
    public MetricsRegistry gauge(@NotNull String name, @NotNull LongSupplier supplier) {
        String fullName = prefix + name;
        gauges.put(fullName, new Gauge(fullName, () -> (double) supplier.getAsLong()));
        return this;
    }

    /**
     * Registers a gauge that returns a double value.
     *
     * @param name the gauge name
     * @param supplier the value supplier
     * @return this registry
     */
    public MetricsRegistry gauge(@NotNull String name, @NotNull DoubleSupplier supplier) {
        String fullName = prefix + name;
        gauges.put(fullName, new Gauge(fullName, supplier));
        return this;
    }

    /**
     * Gets or creates a timer.
     *
     * @param name the timer name
     * @return the timer
     */
    public @NotNull Timer timer(@NotNull String name) {
        String fullName = prefix + name;
        return timers.computeIfAbsent(fullName, Timer::new);
    }

    /**
     * Returns all metrics as a map.
     *
     * @return unmodifiable map of all metrics
     */
    public @NotNull Map<String, Map<String, Object>> getMetrics() {
        Map<String, Map<String, Object>> result = new ConcurrentHashMap<>();

        // Counters
        counters.forEach((name, counter) -> {
            result.put(name, Map.of(
                "type", "counter",
                "value", counter.count()
            ));
        });

        // Gauges
        gauges.forEach((name, gauge) -> {
            result.put(name, Map.of(
                "type", "gauge",
                "value", gauge.value()
            ));
        });

        // Timers
        timers.forEach((name, timer) -> {
            Timer.Snapshot snapshot = timer.snapshot();
            result.put(name, Map.of(
                "type", "timer",
                "count", snapshot.count(),
                "totalTimeMs", snapshot.totalTimeMs(),
                "meanMs", snapshot.meanMs(),
                "maxMs", snapshot.maxMs()
            ));
        });

        return Collections.unmodifiableMap(result);
    }

    /**
     * Clears all metrics.
     */
    public void clear() {
        counters.clear();
        gauges.clear();
        timers.clear();
    }

    // ========== Metric Types ==========

    /**
     * Monotonically increasing counter.
     */
    public static final class Counter {
        private final String name;
        private final LongAdder count = new LongAdder();

        Counter(String name) {
            this.name = name;
        }

        public void increment() {
            count.increment();
        }

        public void increment(long delta) {
            count.add(delta);
        }

        public long count() {
            return count.sum();
        }

        public String name() {
            return name;
        }
    }

    /**
     * Gauge that reports a current value.
     */
    public static final class Gauge {
        private final String name;
        private final DoubleSupplier supplier;

        Gauge(String name, DoubleSupplier supplier) {
            this.name = name;
            this.supplier = supplier;
        }

        public double value() {
            return supplier.getAsDouble();
        }

        public String name() {
            return name;
        }
    }

    /**
     * Timer for measuring durations.
     */
    public static final class Timer {
        private final String name;
        private final LongAdder count = new LongAdder();
        private final LongAdder totalTimeNanos = new LongAdder();
        private final AtomicLong maxTimeNanos = new AtomicLong();

        Timer(String name) {
            this.name = name;
        }

        /**
         * Records a duration.
         *
         * @param durationMs duration in milliseconds
         */
        public void record(long durationMs) {
            long nanos = durationMs * 1_000_000;
            count.increment();
            totalTimeNanos.add(nanos);
            maxTimeNanos.updateAndGet(current -> Math.max(current, nanos));
        }

        /**
         * Records a duration from a runnable.
         *
         * @param runnable the operation to time
         */
        public void record(@NotNull Runnable runnable) {
            long start = System.nanoTime();
            try {
                runnable.run();
            } finally {
                record((System.nanoTime() - start) / 1_000_000);
            }
        }

        /**
         * Records a duration from a supplier.
         *
         * @param supplier the operation to time
         * @return the result
         */
        public <T> T record(@NotNull Supplier<T> supplier) {
            long start = System.nanoTime();
            try {
                return supplier.get();
            } finally {
                record((System.nanoTime() - start) / 1_000_000);
            }
        }

        public Snapshot snapshot() {
            long cnt = count.sum();
            long total = totalTimeNanos.sum();
            return new Snapshot(
                cnt,
                total / 1_000_000.0,
                cnt > 0 ? (total / cnt) / 1_000_000.0 : 0,
                maxTimeNanos.get() / 1_000_000.0
            );
        }

        public String name() {
            return name;
        }

        public record Snapshot(long count, double totalTimeMs, double meanMs, double maxMs) {}
    }
}
