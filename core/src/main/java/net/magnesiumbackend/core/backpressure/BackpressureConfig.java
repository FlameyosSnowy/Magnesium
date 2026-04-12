package net.magnesiumbackend.core.backpressure;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable configuration for the bounded-queue backpressure strategy.
 *
 * <p>Built via {@link Builder}:
 * <pre>{@code
 * BackpressureConfig config = BackpressureConfig.builder()
 *     .queueCapacity(512)
 *     .onReject(RejectionResponse.of(503)
 *         .withBody("Server busy")
 *         .withRetryAfter(Duration.ofSeconds(5)))
 *     .build();
 * }</pre>
 *
 * <h2>How the strategy works</h2>
 * <ol>
 *   <li>A {@link BoundedBackpressureExecutor} wraps the user-supplied {@link java.util.concurrent.Executor}.</li>
 *   <li>Every incoming request tries to enqueue a task. If the internal queue has fewer than
 *       {@code queueCapacity} pending tasks the request proceeds normally.</li>
 *   <li>If the queue is full the request is <em>rejected synchronously</em> on the I/O thread
 *       with the configured {@link RejectionResponse} — no thread is consumed.</li>
 * </ol>
 *
 * <h2>Choosing a capacity</h2>
 * A good starting point is {@code concurrency × average-latency-in-seconds × 1.5}. Too large a
 * queue just delays failures; too small causes premature rejection. Monitor
 * {@link BoundedBackpressureExecutor#queuedTaskCount()} and tune accordingly.
 */
public final class BackpressureConfig {

    /** Default capacity used when none is specified. */
    public static final int DEFAULT_QUEUE_CAPACITY = 256;

    private final int queueCapacity;
    private final RejectionResponse rejectionResponse;

    private BackpressureConfig(Builder b) {
        this.queueCapacity    = b.queueCapacity;
        this.rejectionResponse = b.rejectionResponse;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Maximum number of tasks that may wait in the bounded queue. */
    public int queueCapacity() { return queueCapacity; }

    /** The HTTP response template sent to clients whose requests are rejected. */
    @NotNull public RejectionResponse rejectionResponse() { return rejectionResponse; }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        private int queueCapacity = DEFAULT_QUEUE_CAPACITY;
        private RejectionResponse rejectionResponse = RejectionResponse.serviceUnavailable();

        private Builder() {}

        /**
         * Sets the maximum number of requests that may be queued waiting for a worker thread.
         * Must be &gt; 0.
         */
        public Builder queueCapacity(int capacity) {
            if (capacity <= 0) throw new IllegalArgumentException("queueCapacity must be > 0");
            this.queueCapacity = capacity;
            return this;
        }

        /**
         * Sets the response sent when a request is rejected because the queue is full.
         * Defaults to a plain 503 with no body.
         */
        public Builder onReject(@NotNull RejectionResponse response) {
            this.rejectionResponse = Objects.requireNonNull(response, "rejectionResponse");
            return this;
        }

        /** Builds and validates the configuration. */
        @NotNull
        public BackpressureConfig build() {
            return new BackpressureConfig(this);
        }
    }

    @Override
    public String toString() {
        return "BackpressureConfig{queueCapacity=" + queueCapacity
            + ", onReject=" + rejectionResponse + '}';
    }
}