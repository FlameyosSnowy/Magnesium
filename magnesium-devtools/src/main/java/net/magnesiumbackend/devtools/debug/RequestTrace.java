package net.magnesiumbackend.devtools.debug;

import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captures detailed trace information for a single request.
 *
 * <p>Includes filter execution details, serialization times, route matching,
 * and performance metrics for debugging and profiling.</p>
 */
public final class RequestTrace {
    private static final Logger logger = LoggerFactory.getLogger(RequestTrace.class);

    private final String traceId;
    private final HttpMethod method;
    private final String path;
    private final Instant startTime;
    private volatile Instant endTime;

    // Filter execution tracking
    private final List<FilterTrace> filterTraces = new ArrayList<>(32);
    private volatile FilterTrace currentFilter;

    // Route resolution
    private volatile RouteMatchTrace routeMatch;

    // Serialization tracking
    private volatile SerializationTrace requestSerialization;
    private volatile SerializationTrace responseSerialization;

    // Final result
    private volatile ResponseEntity<?> response;
    private volatile Throwable error;

    // Metadata
    private final Map<String, Object> metadata = new ConcurrentHashMap<>(32);

    public RequestTrace(String traceId, RequestContext request) {
        this.traceId = traceId;
        this.method = request.request().method();
        this.path = request.request().path();
        this.startTime = Instant.now();
    }

    public void recordFilterStart(String filterName, int position) {
        currentFilter = new FilterTrace(filterName, position, Instant.now());
    }

    public void recordFilterEnd(boolean proceeded, ResponseEntity<?> result) {
        if (currentFilter != null) {
            currentFilter.complete(Instant.now(), proceeded, result);
            filterTraces.add(currentFilter);
            currentFilter = null;
        }
    }

    public void recordFilterSkipped(String filterName, String reason) {
        FilterTrace skipped = new FilterTrace(filterName, -1, Instant.now());
        skipped.markSkipped(reason);
        filterTraces.add(skipped);
    }

    public void recordRouteMatch(String matchedPattern, long matchTimeNanos) {
        this.routeMatch = new RouteMatchTrace(matchedPattern, matchTimeNanos);
    }

    public void recordRequestDeserialization(Class<?> targetType, long timeNanos) {
        this.requestSerialization = new SerializationTrace("deserialization", targetType, timeNanos);
    }

    public void recordResponseSerialization(Class<?> sourceType, long timeNanos) {
        this.responseSerialization = new SerializationTrace("serialization", sourceType, timeNanos);
    }

    public void recordError(Throwable error) {
        logger.error("Error during request processing", error);
        this.error = error;
    }

    public void recordResponse(ResponseEntity<?> response) {
        this.response = response;
        this.endTime = Instant.now();
    }

    public void markComplete() {
        this.endTime = Instant.now();
    }

    public Duration getTotalDuration() {
        Instant end = endTime != null ? endTime : Instant.now();
        return Duration.between(startTime, end);
    }

    // Getters
    public String traceId() { return traceId; }
    public HttpMethod method() { return method; }
    public String path() { return path; }
    public Instant startTime() { return startTime; }
    public Instant endTime() { return endTime; }
    public List<FilterTrace> filterTraces() { return new ArrayList<>(filterTraces); }
    public RouteMatchTrace routeMatch() { return routeMatch; }
    public SerializationTrace requestSerialization() { return requestSerialization; }
    public SerializationTrace responseSerialization() { return responseSerialization; }
    public ResponseEntity<?> response() { return response; }
    public Throwable error() { return error; }
    public Map<String, Object> metadata() { return Map.copyOf(metadata); }

    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Returns true if any filter short-circuited the request.
     */
    public boolean hasShortCircuit() {
        for (FilterTrace ft : filterTraces) {
            if (!ft.proceeded()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the filter that short-circuited, or null if none.
     */
    public FilterTrace getShortCircuitFilter() {
        for (FilterTrace ft : filterTraces) {
            if (!ft.proceeded()) {
                return ft;
            }
        }
        return null;
    }

    /**
     * Gets total time spent in filters.
     */
    public Duration getFilterTime() {
        long nanos = 0L;
        for (FilterTrace ft : filterTraces) {
            if (ft.duration() != null) {
                long l = ft.duration().toNanos();
                nanos += l;
            }
        }
        return Duration.ofNanos(nanos);
    }

    /**
     * Gets the slowest filter in this request.
     */
    public FilterTrace getSlowestFilter() {
        boolean seen = false;
        FilterTrace best = null;
        Comparator<FilterTrace> comparator = Comparator.comparingLong(a -> a.duration().toNanos());
        for (FilterTrace ft : filterTraces) {
            if (ft.duration() != null) {
                if (!seen || comparator.compare(ft, best) > 0) {
                    seen = true;
                    best = ft;
                }
            }
        }
        return seen ? best : null;
    }

    @Override
    public String toString() {
        return String.format("RequestTrace[%s %s %s, %d filters, %s]",
            traceId, method, path, filterTraces.size(), getTotalDuration());
    }
}
