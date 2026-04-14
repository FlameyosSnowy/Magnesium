package net.magnesiumbackend.devtools.debug;

import net.magnesiumbackend.core.annotations.GetMapping;
import net.magnesiumbackend.core.annotations.RestController;
import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.ResponseEntity;

import java.util.*;

/**
 * REST controller exposing debugging endpoints for development.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>/debug/traces - Recent request traces</li>
 *   <li>/debug/traces/{id} - Specific trace details</li>
 *   <li>/debug/filters - Filter performance metrics</li>
 *   <li>/debug/routes - Route performance metrics</li>
 *   <li>/debug/summary - Overall performance summary</li>
 * </ul>
 */
@RestController
public class DebugEndpointHandler {

    @GetMapping(path = "/debug/status")
    public ResponseEntity<Map<String, Object>> getDebugStatus() {
        Map<String, Object> status = new LinkedHashMap<>(2);
        status.put("enabled", MagnesiumDebugger.isEnabled());
        status.put("activeTraces", MagnesiumDebugger.getActiveTraces().size());
        return ResponseEntity.ok(status);
    }

    @GetMapping(path = "/debug/traces")
    public ResponseEntity<List<TraceSummary>> getRecentTraces() {
        List<RequestTrace> recentTraces = MagnesiumDebugger.getRecentTraces(100);
        List<TraceSummary> traces = new ArrayList<>(recentTraces.size());
        for (RequestTrace requestTrace : recentTraces) {
            TraceSummary summarize = summarize(requestTrace);
            traces.add(summarize);
        }
        return ResponseEntity.ok(traces);
    }

    @GetMapping(path = "/debug/traces/{id}")
    public ResponseEntity<?> getTraceDetails(String id) {
        // Check active traces first
        Optional<RequestTrace> trace = Optional.empty();
        for (RequestTrace requestTrace : MagnesiumDebugger.getActiveTraces()) {
            if (requestTrace.traceId().equals(id)) {
                trace = Optional.of(requestTrace);
                break;
            }
        }

        if (trace.isEmpty()) {
            Optional<RequestTrace> found = Optional.empty();
            for (RequestTrace t : MagnesiumDebugger.getRecentTraces(1000)) {
                if (t.traceId().equals(id)) {
                    found = Optional.of(t);
                    break;
                }
            }
            trace = found;
        }

        return trace
            .map(this::toDetailedMap)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound());
    }

    @GetMapping(path = "/debug/filters")
    public ResponseEntity<List<FilterMetricsDto>> getFilterMetrics() {
        Map<String, MagnesiumDebugger.FilterMetrics> filterMetrics = MagnesiumDebugger.getFilterMetrics();
        if (filterMetrics.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<FilterMetricsDto> metrics = new ArrayList<>(filterMetrics.size());
        for (Map.Entry<String, MagnesiumDebugger.FilterMetrics> e : filterMetrics.entrySet()) {
            FilterMetricsDto filterMetricsDto = new FilterMetricsDto(
                e.getKey(),
                e.getValue().callCount(),
                e.getValue().avgTimeNanos() / 1_000_000.0,
                e.getValue().shortCircuitCount()
            );
            metrics.add(filterMetricsDto);
        }
        metrics.sort(Comparator.comparingDouble(FilterMetricsDto::avgTimeMs).reversed());
        return ResponseEntity.ok(metrics);
    }

    @GetMapping(path = "/debug/routes")
    public ResponseEntity<List<RouteMetricsDto>> getRouteMetrics() {
        Map<String, MagnesiumDebugger.RouteMetrics> routeMetrics = MagnesiumDebugger.getRouteMetrics();
        if (routeMetrics.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<RouteMetricsDto> metrics = new ArrayList<>(routeMetrics.size());
        for (Map.Entry<String, MagnesiumDebugger.RouteMetrics> e : routeMetrics.entrySet()) {
            RouteMetricsDto routeMetricsDto = new RouteMetricsDto(
                e.getKey(),
                e.getValue().callCount(),
                e.getValue().avgTimeNanos() / 1_000_000.0,
                e.getValue().errorCount()
            );
            metrics.add(routeMetricsDto);
        }
        metrics.sort(Comparator.comparingDouble(RouteMetricsDto::avgTimeMs).reversed());
        return ResponseEntity.ok(metrics);
    }

    @GetMapping(path = "/debug/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        List<Map.Entry<String, MagnesiumDebugger.FilterMetrics>> slowestFiltersEntries = MagnesiumDebugger.getSlowestFilters(5);
        List<Map.Entry<String, MagnesiumDebugger.RouteMetrics>> slowestRoutesEntries = MagnesiumDebugger.getSlowestRoutes(5);
        if (slowestFiltersEntries.isEmpty() && slowestRoutesEntries.isEmpty()) {
            return ResponseEntity.ok(Map.of());
        }

        Map<String, Object> summary = new LinkedHashMap<>(32);

        // Slowest filters
        List<Map<String, Object>> slowestFilters = new ArrayList<>(slowestFiltersEntries.size());
        for (Map.Entry<String, MagnesiumDebugger.FilterMetrics> stringFilterMetricsEntry : slowestFiltersEntries) {
            Map<String, Object> apply = new LinkedHashMap<>(4);
            apply.put("name", stringFilterMetricsEntry.getKey());
            apply.put("avgTimeMs", stringFilterMetricsEntry.getValue().avgTimeNanos() / 1_000_000.0);
            apply.put("calls", stringFilterMetricsEntry.getValue().callCount());
            apply.put("shortCircuits", stringFilterMetricsEntry.getValue().shortCircuitCount());
            slowestFilters.add(apply);
        }
        summary.put("slowestFilters", slowestFilters);

        // Slowest routes
        List<Map<String, Object>> slowestRoutes = new ArrayList<>(slowestFiltersEntries.size());
        for (Map.Entry<String, MagnesiumDebugger.RouteMetrics> stringRouteMetricsEntry : slowestRoutesEntries) {
            Map<String, Object> apply = new LinkedHashMap<>(4);
            apply.put("route", stringRouteMetricsEntry.getKey());
            apply.put("avgTimeMs", stringRouteMetricsEntry.getValue().avgTimeNanos() / 1_000_000.0);
            apply.put("calls", stringRouteMetricsEntry.getValue().callCount());
            apply.put("errors", stringRouteMetricsEntry.getValue().errorCount());
            slowestRoutes.add(apply);
        }
        summary.put("slowestRoutes", slowestRoutes);

        // Recent activity
        List<RequestTrace> recent = MagnesiumDebugger.getRecentTraces(50);
        summary.put("recentRequests", recent.size());
        long shortCircuitedRequestsCount = 0L;
        for (RequestTrace trace : recent) {
            if (trace.hasShortCircuit()) {
                shortCircuitedRequestsCount++;
            }
        }
        summary.put("shortCircuitedRequests", shortCircuitedRequestsCount);
        long errorRequestsCount = 0L;
        for (RequestTrace trace : recent) {
            if (trace.error() != null) {
                errorRequestsCount++;
            }
        }
        summary.put("errorRequests", errorRequestsCount);

        // Average times
        long sum = 0;
        long count = 0;
        for (RequestTrace requestTrace : recent) {
            long millis = requestTrace.getTotalDuration().toMillis();
            sum += millis;
            count++;
        }
        double avgRequestTime = count > 0 ? (double) sum / count : 0.0;
        summary.put("avgRequestTimeMs", avgRequestTime);

        long result = 0;
        long count1 = 0;
        for (RequestTrace t : recent) {
            long millis = t.getFilterTime().toMillis();
            result += millis;
            count1++;
        }
        double avgFilterTime = count1 > 0 ? (double) result / count1 : 0.0;
        summary.put("avgFilterTimeMs", avgFilterTime);

        return ResponseEntity.ok(summary);
    }

    private TraceSummary summarize(RequestTrace trace) {
        return new TraceSummary(
            trace.traceId(),
            trace.method(),
            trace.path(),
            trace.getTotalDuration().toMillis(),
            trace.filterTraces().size(),
            trace.hasShortCircuit(),
            trace.error() != null,
            trace.startTime().toEpochMilli()
        );
    }

    private Map<String, Object> toDetailedMap(RequestTrace trace) {
        Map<String, Object> map = new LinkedHashMap<>(32);
        map.put("traceId", trace.traceId());
        map.put("method", trace.method());
        map.put("path", trace.path());
        map.put("startTime", trace.startTime().toString());
        map.put("durationMs", trace.getTotalDuration().toMillis());
        map.put("filterTimeMs", trace.getFilterTime().toMillis());

        // Filters
        List<FilterTrace> filterTraces = trace.filterTraces();
        List<Map<String, Object>> filters = new ArrayList<>(filterTraces.size());
        for (FilterTrace filterTrace : filterTraces) {
            Map<String, Object> apply = new LinkedHashMap<>(6);
            apply.put("name", filterTrace.filterName());
            apply.put("skipped", filterTrace.skipped());
            if (!filterTrace.skipped()) {
                apply.put("durationMs", filterTrace.duration() != null ? filterTrace.duration().toMillis() : null);
                apply.put("proceeded", filterTrace.proceeded());
                apply.put("shortCircuited", filterTrace.shortCircuited());
            } else {
                apply.put("skipReason", filterTrace.skipReason());
            }
            filters.add(apply);
        }
        map.put("filters", filters);

        // Route match
        if (trace.routeMatch() != null) {
            Map<String, Object> rm = new LinkedHashMap<>(3);
            rm.put("pattern", trace.routeMatch().matchedPattern());
            rm.put("matchTimeMicros", trace.routeMatch().matchTimeNanos() / 1000);
            map.put("routeMatch", rm);
        }

        // Serialization
        if (trace.requestSerialization() != null) {
            Map<String, Object> ser = new LinkedHashMap<>(4);
            ser.put("operation", trace.requestSerialization().operation());
            ser.put("type", trace.requestSerialization().type().getName());
            ser.put("timeMs", trace.requestSerialization().time().toMillis());
            map.put("requestDeserialization", ser);
        }

        if (trace.responseSerialization() != null) {
            Map<String, Object> ser = new LinkedHashMap<>(4);
            ser.put("operation", trace.responseSerialization().operation());
            ser.put("type", trace.responseSerialization().type().getName());
            ser.put("timeMs", trace.responseSerialization().time().toMillis());
            map.put("responseSerialization", ser);
        }

        // Error
        if (trace.error() != null) {
            map.put("error", trace.error().getClass().getName() + ": " + trace.error().getMessage());
        }

        return map;
    }

    // DTO records

    public record TraceSummary(
        String traceId,
        HttpMethod method,
        String path,
        long durationMs,
        int filterCount,
        boolean shortCircuited,
        boolean hasError,
        long timestamp
    ) {}

    public record FilterMetricsDto(
        String name,
        long calls,
        double avgTimeMs,
        long shortCircuits
    ) {}

    public record RouteMetricsDto(
        String route,
        long calls,
        double avgTimeMs,
        long errors
    ) {}
}
