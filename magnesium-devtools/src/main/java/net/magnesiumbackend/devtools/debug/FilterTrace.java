package net.magnesiumbackend.devtools.debug;

import net.magnesiumbackend.core.http.response.ResponseEntity;

import java.time.Duration;
import java.time.Instant;

/**
 * Captures execution details for a single filter invocation.
 */
public final class FilterTrace {
    private final String filterName;
    private final int position;
    private final Instant startTime;
    private volatile Instant endTime;
    private volatile boolean proceeded;
    private volatile ResponseEntity<?> result;
    private volatile Throwable error;

    // For skipped filters
    private volatile boolean skipped;
    private volatile String skipReason;

    FilterTrace(String filterName, int position, Instant startTime) {
        this.filterName = filterName;
        this.position = position;
        this.startTime = startTime;
    }

    void complete(Instant endTime, boolean proceeded, ResponseEntity<?> result) {
        this.endTime = endTime;
        this.proceeded = proceeded;
        this.result = result;
    }

    void markSkipped(String reason) {
        this.skipped = true;
        this.skipReason = reason;
        this.endTime = startTime; // Instant complete
    }

    void markError(Throwable error) {
        this.error = error;
        this.endTime = Instant.now();
    }

    // Getters
    public String filterName() { return filterName; }
    public int position() { return position; }
    public Instant startTime() { return startTime; }
    public Instant endTime() { return endTime; }
    public boolean proceeded() { return proceeded; }
    public ResponseEntity<?> result() { return result; }
    public Throwable error() { return error; }
    public boolean skipped() { return skipped; }
    public String skipReason() { return skipReason; }

    public Duration duration() {
        if (endTime == null) return null;
        return Duration.between(startTime, endTime);
    }

    public boolean shortCircuited() {
        return !proceeded && !skipped && result != null;
    }

    @Override
    public String toString() {
        if (skipped) {
            return String.format("FilterTrace[%s: SKIPPED (%s)]", filterName, skipReason);
        }
        Duration d = duration();
        return String.format("FilterTrace[%s: %s, %s]",
            filterName,
            proceeded ? "proceeded" : "short-circuited",
            d != null ? d.toMillis() + "ms" : "incomplete");
    }
}
