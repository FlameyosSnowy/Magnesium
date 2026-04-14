package net.magnesiumbackend.devtools.debug;

import java.time.Duration;

/**
 * Captures route matching performance data.
 */
public final class RouteMatchTrace {
    private final String matchedPattern;
    private final long matchTimeNanos;

    RouteMatchTrace(String matchedPattern, long matchTimeNanos) {
        this.matchedPattern = matchedPattern;
        this.matchTimeNanos = matchTimeNanos;
    }

    public String matchedPattern() { return matchedPattern; }
    public long matchTimeNanos() { return matchTimeNanos; }

    public Duration matchTime() {
        return Duration.ofNanos(matchTimeNanos);
    }

    @Override
    public String toString() {
        return String.format("RouteMatch[%s, %dμs]", matchedPattern, matchTimeNanos / 1000);
    }
}
