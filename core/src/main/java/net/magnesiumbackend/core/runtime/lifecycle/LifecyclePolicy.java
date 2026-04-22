package net.magnesiumbackend.core.runtime.lifecycle;

import org.jetbrains.annotations.NotNull;

/**
 * Lifecycle policy for the runtime kernel.
 *
 * <p>Determines how the main thread blocks until shutdown.</p>
 *
 * <h3>Built-in Policies</h3>
 * <ul>
 *   <li>{@link ShellLifecyclePolicy} - Shell owns main thread (stdin)</li>
 *   <li>{@link LatchLifecyclePolicy} - CountDownLatch blocks</li>
 *   <li>{@link ParkLifecyclePolicy} - LockSupport.park() blocks</li>
 *   <li>{@link SignalLifecyclePolicy} - OS signal handling</li>
 * </ul>
 */
@FunctionalInterface
public interface LifecyclePolicy {

    /**
     * Blocks until shutdown is requested.
     *
     * <p>Called by the runtime kernel after all input sources are started.
     * This method should block the calling thread until the runtime should
     * shut down (e.g., on SIGINT, shell exit, or latch countdown).</p>
     *
     * @param context the shutdown context for triggering shutdown
     */
    void blockUntilShutdown(@NotNull ShutdownContext context);

    /**
     * Context provided to lifecycle policies for triggering shutdown.
     */
    interface ShutdownContext {
        /**
         * Initiates graceful shutdown.
         */
        void requestShutdown();

        /**
         * Returns true if shutdown has been requested.
         *
         * @return shutdown status
         */
        boolean isShutdownRequested();
    }
}
