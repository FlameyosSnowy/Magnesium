package net.magnesiumbackend.core.backpressure;

/**
 * Thrown by {@link BoundedBackpressureExecutor} when the bounded queue is full and a new
 * task cannot be accepted.
 *
 * <p>This is an {@link Error} subclass, not an {@link Exception}, so it bypasses
 * {@code catch (Exception e)} blocks in the filter/handler chain and surfaces cleanly
 * at the transport layer where it is caught explicitly.
 *
 * <p>Transports must catch {@code QueueRejectedError} <em>before</em> submitting work to
 * the executor and reply with the configured {@link RejectionResponse} immediately on the
 * I/O thread — no worker thread is allocated for a rejected request.
 */
public final class QueueRejectedError extends Error {

    private final BackpressureConfig config;

    public QueueRejectedError(BackpressureConfig config) {
        super("Request queue is full (capacity=" + config.queueCapacity() + ")", null,
              true,  // enableSuppression
              false  // writableStackTrace — keep it cheap, this is a hot path
        );
        this.config = config;
    }

    /** The backpressure configuration that produced this rejection. */
    public BackpressureConfig config() { return config; }

    /** Convenience accessor for the configured rejection response. */
    public RejectionResponse rejectionResponse() { return config.rejectionResponse(); }
}