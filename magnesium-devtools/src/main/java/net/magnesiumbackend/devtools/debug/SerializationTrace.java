package net.magnesiumbackend.devtools.debug;

import java.time.Duration;

/**
 * Captures serialization/deserialization performance data.
 */
public final class SerializationTrace {
    private final String operation; // "serialization" or "deserialization"
    private final Class<?> type;
    private final long timeNanos;

    SerializationTrace(String operation, Class<?> type, long timeNanos) {
        this.operation = operation;
        this.type = type;
        this.timeNanos = timeNanos;
    }

    public String operation() { return operation; }
    public Class<?> type() { return type; }
    public long timeNanos() { return timeNanos; }

    public Duration time() {
        return Duration.ofNanos(timeNanos);
    }

    @Override
    public String toString() {
        return String.format("Serialization[%s %s, %dμs]",
            operation, type.getSimpleName(), timeNanos / 1000);
    }
}
