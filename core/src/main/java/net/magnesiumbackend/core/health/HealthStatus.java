package net.magnesiumbackend.core.health;

import org.jetbrains.annotations.NotNull;

/**
 * Health status values.
 *
 * <p>Ordered by severity: UP < UNKNOWN < OUT_OF_SERVICE < DOWN</p>
 */
public enum HealthStatus {

    /**
     * Component is functioning normally.
     */
    UP(0, "UP"),

    /**
     * Status cannot be determined.
     */
    UNKNOWN(1, "UNKNOWN"),

    /**
     * Component is intentionally offline (e.g., maintenance mode).
     */
    OUT_OF_SERVICE(2, "OUT_OF_SERVICE"),

    /**
     * Component has failed.
     */
    DOWN(3, "DOWN");

    private final int severity;
    private final String code;

    HealthStatus(int severity, String code) {
        this.severity = severity;
        this.code = code;
    }

    /**
     * Returns the severity level (higher = more severe).
     *
     * @return severity level
     */
    public int severity() {
        return severity;
    }

    /**
     * Returns the status code string.
     *
     * @return status code
     */
    public @NotNull String code() {
        return code;
    }

    /**
     * Combines two statuses, returning the one with higher severity.
     *
     * @param other the other status
     * @return the more severe status
     */
    public HealthStatus combine(HealthStatus other) {
        return this.severity >= other.severity ? this : other;
    }

    @Override
    public String toString() {
        return code;
    }
}
