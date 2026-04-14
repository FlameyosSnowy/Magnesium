package net.magnesiumbackend.core.ratelimit;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {
    public enum Algorithm { FIXED_WINDOW, SLIDING_WINDOW, TOKEN_BUCKET }

    private final int      requests;
    private final Duration window;
    private final Algorithm algorithm;
    private final ConcurrentHashMap<String, WindowState> windows = new ConcurrentHashMap<>();

    private RateLimiter(Builder b) {
        this.requests  = b.requests;
        this.window    = b.window;
        this.algorithm = b.algorithm;
    }

    /**
     * Returns a RateLimitResult for the given key (e.g. IP, user ID, API key).
     */
    public RateLimitResult check(String key) {
        WindowState state = windows.computeIfAbsent(key, k -> new WindowState(requests, window, algorithm));
        return state.consume();
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() { return new Builder(); }

    public static final class Builder {
        private int      requests  = 100;
        private Duration window    = Duration.ofMinutes(1);
        private Algorithm algorithm = Algorithm.SLIDING_WINDOW;

        public Builder requests(int v) {
            this.requests = v;
            return this;
        }

        public Builder window(Duration v) {
            this.window = v;
            return this;
        }

        public Builder fixedWindow() {
            this.algorithm = Algorithm.FIXED_WINDOW;
            return this;
        }
        public Builder slidingWindow() {
            this.algorithm = Algorithm.SLIDING_WINDOW;
            return this;
        }
        public Builder tokenBucket() {
            this.algorithm = Algorithm.TOKEN_BUCKET;
            return this;
        }
        
        public RateLimiter build() {
            return new RateLimiter(this);
        }
    }
}