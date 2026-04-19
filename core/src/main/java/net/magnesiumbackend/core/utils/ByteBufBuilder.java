package net.magnesiumbackend.core.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * A pooled, resizable byte buffer builder optimized for request-scoped operations.
 *
 * <p>ByteBufBuilder uses a {@link ThreadLocal} pool to avoid repeated allocations
 * during request processing. Instances are acquired via {@link #acquire(int)} and
 * should always be used with try-with-resources:</p>
 *
 * <pre>{@code
 * try (ByteBufBuilder buf = ByteBufBuilder.acquire(256)) {
 *     buf.append("Content-Type");
 *     buf.append(':');
 *     buf.append("application/json");
 *     byte[] result = buf.copyAndRelease();
 *     // use result...
 * }
 * }</pre>
 *
 * <p>The pool maintains one instance per thread with a default capacity of 512 bytes.
 * If a request requires more space, the buffer grows and the larger capacity is
 * retained for subsequent requests on that thread.</p>
 *
 * @see #acquire(int)
 * @see #copyAndRelease()
 */
public final class ByteBufBuilder implements AutoCloseable {

    // Default capacity covers ~90% of typical HTTP header blocks
    private static final int DEFAULT_CAPACITY = 512;
    private static final int MAX_POOLED_CAPACITY = 8192; // Don't retain huge buffers

    private static final ThreadLocal<ByteBufBuilder> POOL = ThreadLocal.withInitial(() ->
        new ByteBufBuilder(DEFAULT_CAPACITY, true)
    );

    private byte[] buf;
    private int pos;
    private final boolean pooled;
    private boolean acquired;

    /**
     * Acquires a pooled ByteBufBuilder from the thread-local pool.
     *
     * <p>If the pooled instance has sufficient capacity, it is reset and returned.
     * Otherwise, a non-pooled instance is created. The returned instance must be
     * closed via try-with-resources or {@link #close()}.</p>
     *
     * @param minCapacity the minimum required capacity
     * @return a ready-to-use ByteBufBuilder
     */
    public static ByteBufBuilder acquire(int minCapacity) {
        ByteBufBuilder pooled = POOL.get();
        if (pooled.buf.length >= minCapacity && !pooled.acquired) {
            pooled.reset();
            pooled.acquired = true;
            return pooled;
        }
        // Fall back to non-pooled for oversized requests or nested usage
        return new ByteBufBuilder(Math.max(minCapacity, 64), false);
    }

    /**
     * Convenience method to acquire with default capacity.
     *
     * @return a pooled ByteBufBuilder with at least 512 bytes capacity
     */
    public static ByteBufBuilder acquire() {
        return acquire(DEFAULT_CAPACITY);
    }

    private ByteBufBuilder(int initialCapacity, boolean pooled) {
        this.buf = new byte[initialCapacity];
        this.pooled = pooled;
    }

    private void reset() {
        this.pos = 0;
    }

    /**
     * Returns the builder to the pool (for try-with-resources).
     *
     * <p>This is automatically called when using try-with-resources.
     * The internal buffer remains available for reuse by subsequent
     * requests on the same thread.</p>
     */
    @Override
    public void close() {
        if (pooled && acquired) {
            acquired = false;
            pos = 0;
            // Trim if buffer grew too large
            if (buf.length > MAX_POOLED_CAPACITY) {
                buf = new byte[DEFAULT_CAPACITY];
            }
        }
    }

    /**
     * Copies the current content and releases the builder to the pool.
     *
     * <p>This is the recommended way to extract data when pooling.
     * It returns a right-sized copy of the data and returns the
     * (potentially larger) internal buffer to the pool for reuse.</p>
     *
     * @return a new byte array containing exactly the written data
     */
    public byte @NotNull [] copyAndRelease() {
        return Arrays.copyOf(buf, pos);
    }

    public void append(@NotNull String s) {
        int len = s.length();
        ensure(len);

        for (int i = 0; i < len; i++) {
            buf[pos++] = (byte) s.charAt(i);
        }
    }

    public void appendAscii(@NotNull String s) {
        int len = s.length();
        ensure(len);

        s.getBytes(0, len, buf, pos);
        pos += len;
    }

    public void append(byte b) {
        ensure(1);
        buf[pos++] = b;
    }

    public void append(char c) {
        ensure(1);
        buf[pos++] = (byte) (c & 0xFF);
    }

    public void append(byte[] bytes) {
        ensure(bytes.length);
        System.arraycopy(bytes, 0, buf, pos, bytes.length);
        pos += bytes.length;
    }

    @Contract(value = " -> new", pure = true)
    public byte @NotNull [] build() {
        return buf;
    }

    private void ensure(int needed) {
        int required = pos + needed;
        if (required > buf.length) {
            int newCap = Math.max(buf.length << 1, required);
            buf = Arrays.copyOf(buf, newCap);
        }
    }

    public void setLength(int length) {
        pos = length;
    }

    public void append(byte[] array, int index, int length) {
        ensure(length);
        System.arraycopy(array, index, buf, pos, length);
        pos += length;
    }
}