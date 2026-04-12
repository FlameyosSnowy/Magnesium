package net.magnesiumbackend.core.cancellation;

/**
 * Token for propagating cancellation requests across async operations.
 *
 * <p>Handlers and filters can check {@link #isCancelled()} to cooperatively
 * abort long-running operations. Transports create and manage tokens per-request.</p>
 *
 * <p>Example usage in a handler:</p>
 * <pre>
 * for (int i = 0; i < largeDataset.size(); i++) {
 *     if (ctx.cancellationToken().isCancelled()) {
 *         return ResponseEntity.status(499).body("Client closed request");
 *     }
 *     process(largeDataset.get(i));
 * }
 * </pre>
 */
public interface CancellationToken {

    /** Returns true if cancellation has been requested. */
    boolean isCancelled();

    /** Throws CancellationException if cancelled, otherwise returns normally. */
    default void throwIfCancelled() {
        if (isCancelled()) {
            throw new CancellationException("Request was cancelled");
        }
    }

    /** Default uncancellable token for sync operations or tests. */
    static CancellationToken uncancellable() {
        return () -> false;
    }
}
