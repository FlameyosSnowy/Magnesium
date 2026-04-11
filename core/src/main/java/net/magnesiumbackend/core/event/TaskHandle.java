package net.magnesiumbackend.core.event;

import java.util.concurrent.TimeUnit;

/**
 * A handle to a scheduled {@link net.magnesiumbackend.core.event.Task} returned by
 * {@link net.magnesiumbackend.core.event.EventBus#schedule}.
 *
 * <p>The caller holds this to inspect or cancel the running task.
 * The underlying executor and future are managed by {@code EventBus},
 * they are not exposed here.
 *
 * <pre>{@code
 * TaskHandle handle = eventBus.schedule(task);
 *
 * handle.isRunning();      // check status
 * handle.cancel();         // graceful stop, waits up to 5s
 * handle.cancel(10, SECONDS); // graceful stop with explicit timeout
 * }</pre>
 */
public interface TaskHandle {

    /** The name of the underlying {@link Task}. */
    String name();

    /** Returns {@code true} if the task is still scheduled and has not been cancelled. */
    boolean isRunning();

    /**
     * Requests a graceful stop. The in-flight execution (if any) is allowed to finish.
     * Waits up to 5 seconds for termination.
     *
     * @return {@code true} if the executor terminated cleanly within the timeout
     */
    boolean cancel();

    /**
     * Requests a graceful stop with an explicit timeout.
     *
     * @param timeout how long to wait for in-flight execution to finish
     * @param unit    the time unit of {@code timeout}
     * @return {@code true} if the executor terminated cleanly within the timeout
     */
    boolean cancel(long timeout, TimeUnit unit);
}