package net.magnesiumbackend.core.runtime.lifecycle;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

/**
 * Lifecycle policy using LockSupport.park().
 *
 * <p>Parks the thread until {@link #unpark()} is called. Low-overhead
 * blocking suitable for daemon-like services.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * ParkLifecyclePolicy policy = new ParkLifecyclePolicy();
 *
 * RuntimeConfig config = RuntimeConfig.builder()
 *     .lifecyclePolicy(policy)
 *     .build();
 *
 * // Later, from another thread:
 * policy.unpark();
 * }</pre>
 */
public final class ParkLifecyclePolicy implements LifecyclePolicy {

    private static final Logger logger = Logger.getLogger(ParkLifecyclePolicy.class.getName());

    private volatile Thread parkedThread;

    @Override
    public void blockUntilShutdown(@NotNull ShutdownContext context) {
        parkedThread = Thread.currentThread();

        while (!context.isShutdownRequested()) {
            LockSupport.park();

            // Handle spurious wakeups
            if (Thread.interrupted()) {
                context.requestShutdown();
                break;
            }
        }

        parkedThread = null;
    }

    /**
     * Unparks the blocked thread, triggering shutdown.
     */
    public void unpark() {
        Thread thread = parkedThread;
        if (thread != null) {
            LockSupport.unpark(thread);
        }
    }
}
