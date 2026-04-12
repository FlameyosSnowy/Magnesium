package net.magnesiumbackend.core.backpressure;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@link Executor} decorator that enforces a bounded queue in front of a delegate executor.
 *
 * <h2>Mechanics</h2>
 * <ul>
 *   <li>An {@link ArrayBlockingQueue} of fixed capacity tracks <em>pending</em> tasks (those
 *       submitted to the delegate but not yet started).</li>
 *   <li>When {@link #execute(Runnable)} is called, the task is offered to the queue.
 *       <ul>
 *         <li>If the queue has space the task is accepted, delegated, and a completion hook
 *             drains the slot on finish.</li>
 *         <li>If the queue is full {@link QueueRejectedError} is thrown <em>synchronously</em>
 *             on the calling (I/O) thread — the delegate is never touched.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h2>Why a separate tracking queue instead of wrapping ThreadPoolExecutor?</h2>
 * <p>Magnesium lets callers supply <em>any</em> {@link Executor} (virtual thread factories,
 * ForkJoinPool, custom schedulers). We cannot introspect an arbitrary executor's internal
 * queue depth, so we maintain our own lightweight counter. The {@link ArrayBlockingQueue} is
 * used purely as an atomic, bounded slot pool — tasks are never actually <em>stored</em> in
 * it beyond a sentinel token that is removed as soon as the delegate accepts the work.</p>
 *
 * <h2>Thread safety</h2>
 * All public methods are thread-safe. The slot acquisition/release cycle is atomic with
 * respect to the queue's {@code offer}/{@code poll} operations.
 *
 * <h2>Observable metrics</h2>
 * <ul>
 *   <li>{@link #queuedTaskCount()} — current number of inflight + pending tasks.</li>
 *   <li>{@link #rejectedTaskCount()} — total tasks rejected since creation.</li>
 *   <li>{@link #capacity()} — the configured queue capacity.</li>
 * </ul>
 */
public final class BoundedBackpressureExecutor implements Executor {

    private final Executor delegate;
    private final BackpressureConfig config;

    /**
     * Slot pool: a token (Boolean.TRUE) is inserted when a task is accepted and removed
     * when the task completes. Capacity == queueCapacity.
     */
    private final BlockingQueue<Boolean> slots;

    /** Total rejections since creation. */
    private final AtomicInteger rejectedCount = new AtomicInteger();

    public BoundedBackpressureExecutor(@NotNull Executor delegate, @NotNull BackpressureConfig config) {
        this.delegate = delegate;
        this.config   = config;
        this.slots    = new ArrayBlockingQueue<>(config.queueCapacity());
    }

    // -------------------------------------------------------------------------
    // Executor contract
    // -------------------------------------------------------------------------

    /**
     * Submits a task to the delegate executor if a queue slot is available.
     *
     * @throws QueueRejectedError if the bounded queue is full — thrown <em>synchronously</em>
     *                            on the calling thread, before touching the delegate.
     */
    @Override
    public void execute(@NotNull Runnable command) {
        // Try to claim a slot — non-blocking
        boolean acquired = slots.offer(Boolean.TRUE);
        if (!acquired) {
            rejectedCount.incrementAndGet();
            throw new QueueRejectedError(config);
        }

        try {
            delegate.execute(wrap(command));
        } catch (RejectedExecutionException ex) {
            // The underlying executor itself rejected — release slot and propagate as our error
            slots.poll();
            rejectedCount.incrementAndGet();
            throw new QueueRejectedError(config);
        }
    }

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    /** Current number of tasks occupying a queue slot (queued or executing). */
    public int queuedTaskCount() { return slots.size(); }

    /** Total tasks rejected since this executor was created. */
    public int rejectedTaskCount() { return rejectedCount.get(); }

    /** Configured maximum queue capacity. */
    public int capacity() { return config.queueCapacity(); }

    /** Load factor in [0.0, 1.0]. Values close to 1.0 indicate sustained pressure. */
    public double loadFactor() { return (double) slots.size() / config.queueCapacity(); }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    /**
     * Wraps the task so the slot is released regardless of how the task exits
     * (normal completion, exception, or {@link Error}).
     */
    private Runnable wrap(Runnable task) {
        return () -> {
            try {
                task.run();
            } finally {
                slots.poll();
            }
        };
    }
}