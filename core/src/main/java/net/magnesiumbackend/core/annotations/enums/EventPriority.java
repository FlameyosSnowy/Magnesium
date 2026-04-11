package net.magnesiumbackend.core.annotations.enums;

import net.magnesiumbackend.core.exceptions.MonitorCancellationException;

public enum EventPriority {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST,

    /**
     * Runs after all other priorities, including {@link #LOWEST}.
     *
     * <p>Intended for listeners that need to observe the final settled state of an
     * event, logging, auditing, metrics, without influencing its outcome.
     *
     * <p>Two guarantees apply at this priority:
     * <ul>
     *   <li>The listener will run even if the event was canceled by a higher priority.
     *   <li>The listener may not cancel the event. Attempting to do so throws
     *       {@link MonitorCancellationException}.
     * </ul>
     */
    MONITOR
}