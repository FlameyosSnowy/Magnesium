package net.magnesiumbackend.core.cancellation;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple, thread-safe cancellation token implementation.
 *
 * <p>Used by transports to create cancellable request contexts that can be
 * triggered when the client disconnects or the request times out.</p>
 */
public final class SimpleCancellationToken implements CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Requests cancellation. Idempotent - subsequent calls have no effect.
     */
    public void cancel() {
        cancelled.set(true);
    }
}
