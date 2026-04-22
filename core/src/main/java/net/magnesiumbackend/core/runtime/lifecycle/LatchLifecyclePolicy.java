package net.magnesiumbackend.core.runtime.lifecycle;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;

/**
 * Lifecycle policy using a CountDownLatch.
 *
 * <p>Blocks until {@link #countDown()} is called. Useful for HTTP servers
 * that need to block the main thread until shutdown is requested.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // In HTTP server: blocks main thread
 * RuntimeConfig config = RuntimeConfig.builder()
 *     .lifecyclePolicy(new LatchLifecyclePolicy())
 *     .build();
 *
 * // Shutdown via API call or signal handler
 * latchPolicy.countDown();
 * }</pre>
 */
public final class LatchLifecyclePolicy implements LifecyclePolicy {

    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void blockUntilShutdown(@NotNull ShutdownContext context) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            context.requestShutdown();
        }
    }

    /**
     * Releases the latch, allowing shutdown to proceed.
     */
    public void countDown() {
        latch.countDown();
    }

    /**
     * Returns the current count of the latch.
     *
     * @return latch count
     */
    public long getCount() {
        return latch.getCount();
    }
}
