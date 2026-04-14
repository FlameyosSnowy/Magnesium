package net.magnesiumbackend.devtools.debug;

import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Central debugging and profiling facade for Magnesium.
 *
 * <p>Provides request tracing, filter debugging, and route performance profiling
 * for development and debugging scenarios.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Enable debugging
 * MagnesiumDebugger.enable();
 *
 * // Get current request trace
 * debugger.getCurrentTrace().ifPresent(trace -> {
 *     logger.info("Request: " + trace.method() + " " + trace.path());
 *     logger.info("Filters: " + trace.filterTraces().size());
 * });
 *
 * // Print performance report
 * debugger.printPerformanceReport();
 * }</pre>
 */
public final class MagnesiumDebugger {
    private static final Logger logger = LoggerFactory.getLogger(MagnesiumDebugger.class);

    private static volatile boolean enabled = false;
    private static final ThreadLocal<RequestTrace> currentTrace = new ThreadLocal<>();
    private static final Map<String, RequestTrace> activeTraces = new ConcurrentHashMap<>(32);
    private static final Queue<RequestTrace> completedTraces = new ConcurrentLinkedQueue<>();
    private static final int MAX_COMPLETED_TRACES = 1000;

    // Performance aggregations
    private static final ConcurrentHashMap<String, FilterMetrics> filterMetrics = new ConcurrentHashMap<>(32);
    private static final ConcurrentHashMap<String, RouteMetrics> routeMetrics = new ConcurrentHashMap<>(32);

    // Listeners
    private static final List<Consumer<RequestTrace>> traceListeners = new CopyOnWriteArrayList<>();

    private MagnesiumDebugger() {}

    /**
     * Enables request tracing and debugging.
     */
    public static void enable() {
        enabled = true;
    }

    /**
     * Disables request tracing and debugging.
     */
    public static void disable() {
        enabled = false;
    }

    /**
     * Returns true if debugging is enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Starts a new request trace.
     */
    public static RequestTrace startTrace(String traceId, RequestContext request) {
        if (!enabled) return null;

        RequestTrace trace = new RequestTrace(traceId, request);
        currentTrace.set(trace);
        activeTraces.put(traceId, trace);
        return trace;
    }

    /**
     * Gets the current thread's request trace.
     */
    public static Optional<RequestTrace> getCurrentTrace() {
        return Optional.ofNullable(currentTrace.get());
    }

    /**
     * Completes the current request trace.
     */
    public static void completeTrace(String traceId) {
        RequestTrace trace = activeTraces.remove(traceId);
        if (trace != null) {
            trace.markComplete();
            addCompletedTrace(trace);
            updateMetrics(trace);
            notifyListeners(trace);
        }
        if (currentTrace.get() != null && currentTrace.get().traceId().equals(traceId)) {
            currentTrace.remove();
        }
    }

    /**
     * Records that a filter is starting execution.
     */
    public static void filterStart(String filterName, int position) {
        getCurrentTrace().ifPresent(t -> t.recordFilterStart(filterName, position));
    }

    /**
     * Records that a filter has completed.
     */
    public static void filterEnd(boolean proceeded, ResponseEntity<?> result) {
        getCurrentTrace().ifPresent(t -> t.recordFilterEnd(proceeded, result));
    }

    /**
     * Records that a filter was skipped.
     */
    public static void filterSkipped(String filterName, String reason) {
        getCurrentTrace().ifPresent(t -> t.recordFilterSkipped(filterName, reason));
    }

    /**
     * Records route matching performance.
     */
    public static void routeMatched(String pattern, long matchTimeNanos) {
        getCurrentTrace().ifPresent(t -> t.recordRouteMatch(pattern, matchTimeNanos));
    }

    /**
     * Records request deserialization time.
     */
    public static void requestDeserialized(Class<?> type, long timeNanos) {
        getCurrentTrace().ifPresent(t -> t.recordRequestDeserialization(type, timeNanos));
    }

    /**
     * Records response serialization time.
     */
    public static void responseSerialized(Class<?> type, long timeNanos) {
        getCurrentTrace().ifPresent(t -> t.recordResponseSerialization(type, timeNanos));
    }

    /**
     * Records an error in the current request.
     */
    public static void recordError(Throwable error) {
        getCurrentTrace().ifPresent(t -> t.recordError(error));
    }

    /**
     * Records the response for the current request.
     */
    public static void recordResponse(ResponseEntity<?> response) {
        getCurrentTrace().ifPresent(t -> t.recordResponse(response));
    }

    /**
     * Adds a listener that receives completed traces.
     */
    public static void addTraceListener(Consumer<RequestTrace> listener) {
        traceListeners.add(listener);
    }

    /**
     * Removes a trace listener.
     */
    public static void removeTraceListener(Consumer<RequestTrace> listener) {
        traceListeners.remove(listener);
    }

    /**
     * Gets all active (in-progress) traces.
     */
    public static Collection<RequestTrace> getActiveTraces() {
        return new ArrayList<>(activeTraces.values());
    }

    /**
     * Gets recent completed traces.
     */
    public static List<RequestTrace> getRecentTraces(int limit) {
        List<RequestTrace> toSort = new ArrayList<>(completedTraces);
        toSort.sort(Comparator.comparing(RequestTrace::startTime).reversed());
        List<RequestTrace> list = new ArrayList<>(toSort);
        for (RequestTrace completedTrace : toSort) {
            if (limit-- == 0) break;
            list.add(completedTrace);
        }
        return list;
    }

    /**
     * Gets metrics for all filters.
     */
    public static Map<String, FilterMetrics> getFilterMetrics() {
        return new HashMap<>(filterMetrics);
    }

    /**
     * Gets metrics for all routes.
     */
    public static Map<String, RouteMetrics> getRouteMetrics() {
        return new HashMap<>(routeMetrics);
    }

    /**
     * Gets the slowest filters by average execution time.
     */
    public static List<Map.Entry<String, FilterMetrics>> getSlowestFilters(int limit) {
        List<Map.Entry<String, FilterMetrics>> toSort = new ArrayList<>(filterMetrics.entrySet());
        toSort.sort(Comparator.comparingDouble(e -> -e.getValue().avgTimeNanos()));
        List<Map.Entry<String, FilterMetrics>> list = new ArrayList<>(toSort.size());
        for (Map.Entry<String, FilterMetrics> entry : toSort) {
            if (limit-- == 0) break;
            list.add(entry);
        }
        return list;
    }

    /**
     * Gets the slowest routes by average execution time.
     */
    public static List<Map.Entry<String, RouteMetrics>> getSlowestRoutes(int limit) {
        List<Map.Entry<String, RouteMetrics>> toSort = new ArrayList<>(routeMetrics.entrySet());
        toSort.sort(Comparator.comparingDouble(e -> -e.getValue().avgTimeNanos()));
        List<Map.Entry<String, RouteMetrics>> list = new ArrayList<>(toSort);
        for (Map.Entry<String, RouteMetrics> entry : toSort) {
            if (limit-- == 0) break;
            list.add(entry);
        }
        return list;
    }

    /**
     * Clears all stored traces and metrics.
     */
    public static void clear() {
        activeTraces.clear();
        completedTraces.clear();
        filterMetrics.clear();
        routeMetrics.clear();
    }

    /**
     * Prints a performance report to logger.
     */
    public static void printPerformanceReport() {
        logger.info("\n=== Magnesium Performance Report ===\n");

        // Slowest routes
        logger.info("Slowest Routes (avg time):");
        getSlowestRoutes(10).forEach(entry -> {
            RouteMetrics m = entry.getValue();
            logger.info("  {}: {}ms (calls: {}, errors: {})",
                entry.getKey(), m.avgTimeNanos() / 1_000_000.0, m.callCount(), m.errorCount());
        });

        // Slowest filters
        logger.info("Slowest Filters (avg time):");
        getSlowestFilters(10).forEach(entry -> {
            FilterMetrics m = entry.getValue();
            logger.info("  {}: {}ms (calls: {}, short-circuits: {})",
                entry.getKey(), m.avgTimeNanos() / 1_000_000.0, m.callCount(), m.shortCircuitCount());
        });

        // Recent traces summary
        logger.info("Recent Request Summary:");
        long totalRequests = completedTraces.size();
        long shortCircuited = 0L;
        for (RequestTrace completedTrace : completedTraces) {
            if (completedTrace.hasShortCircuit()) {
                shortCircuited++;
            }
        }
        long errors = 0L;
        for (RequestTrace t : completedTraces) {
            if (t.error() != null) {
                errors++;
            }
        }

        logger.info("  Total completed: {}", totalRequests);
        logger.info("  Short-circuited: {} ({})", shortCircuited, 100.0 * shortCircuited / totalRequests);
        logger.info("  With errors: {} ({})", errors, 100.0 * errors / totalRequests);

        logger.info("\n====================================\n");
    }

    // Internal helpers

    private static void addCompletedTrace(RequestTrace trace) {
        completedTraces.add(trace);
        // Trim old traces
        while (completedTraces.size() > MAX_COMPLETED_TRACES) {
            completedTraces.poll();
        }
    }

    private static void updateMetrics(RequestTrace trace) {
        // Update route metrics
        String routeKey = trace.method() + " " + trace.path();
        RouteMetrics rm = routeMetrics.computeIfAbsent(routeKey, k -> new RouteMetrics());
        rm.record(trace.getTotalDuration().toNanos(), trace.error() != null);

        // Update filter metrics
        for (FilterTrace ft : trace.filterTraces()) {
            if (ft.skipped()) continue;
            FilterMetrics fm = filterMetrics.computeIfAbsent(ft.filterName(), k -> new FilterMetrics());
            Duration d = ft.duration();
            if (d != null) {
                fm.record(d.toNanos(), ft.shortCircuited());
            }
        }
    }

    private static void notifyListeners(RequestTrace trace) {
        for (Consumer<RequestTrace> listener : traceListeners) {
            try {
                listener.accept(trace);
            } catch (Exception e) {
                // Ignore listener errors
            }
        }
    }

    /**
     * Aggregate metrics for a filter.
     */
    public static final class FilterMetrics {
        private final LongAdder callCount = new LongAdder();
        private final LongAdder totalTimeNanos = new LongAdder();
        private final LongAdder shortCircuitCount = new LongAdder();

        void record(long timeNanos, boolean shortCircuited) {
            callCount.increment();
            totalTimeNanos.add(timeNanos);
            if (shortCircuited) {
                shortCircuitCount.increment();
            }
        }

        public long callCount() { return callCount.sum(); }
        public long totalTimeNanos() { return totalTimeNanos.sum(); }
        public long shortCircuitCount() { return shortCircuitCount.sum(); }

        public double avgTimeNanos() {
            long count = callCount.sum();
            return count == 0 ? 0 : (double) totalTimeNanos.sum() / count;
        }
    }

    /**
     * Aggregate metrics for a route.
     */
    public static final class RouteMetrics {
        private final LongAdder callCount = new LongAdder();
        private final LongAdder totalTimeNanos = new LongAdder();
        private final LongAdder errorCount = new LongAdder();

        void record(long timeNanos, boolean error) {
            callCount.increment();
            totalTimeNanos.add(timeNanos);
            if (error) {
                errorCount.increment();
            }
        }

        public long callCount() { return callCount.sum(); }
        public long totalTimeNanos() { return totalTimeNanos.sum(); }
        public long errorCount() { return errorCount.sum(); }

        public double avgTimeNanos() {
            long count = callCount.sum();
            return count == 0 ? 0 : (double) totalTimeNanos.sum() / count;
        }
    }
}
