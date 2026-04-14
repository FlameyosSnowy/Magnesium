package net.magnesiumbackend.core.ratelimit;

import net.magnesiumbackend.core.auth.Principal;
import net.magnesiumbackend.core.http.response.ErrorResponse;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;

public final class RateLimiterFilter implements HttpFilter {

    private static final @org.jetbrains.annotations.NotNull Slice SLICE_ZERO = Slice.of("0");

    @FunctionalInterface
    public interface KeyResolver {
        String resolve(RequestContext ctx);

        static KeyResolver byIp() {
            return ctx -> {
                Slice xff = ctx.headerRaw("X-Forwarded-For");

                if (xff != null && xff.length() > 0) {
                    Slice first = firstToken(xff, ',');
                    Slice trimmed = trim(first);
                    return trimmed.materialize();
                }

                Slice realIp = ctx.headerRaw("X-Real-IP");
                if (realIp != null && realIp.length() > 0) {
                    return trim(realIp).materialize();
                }

                return "unknown";
            };
        }

        static KeyResolver byUser() {
            return ctx -> {
                Principal p = ctx.principal();
                return p.isAnonymous()
                    ? byIp().resolve(ctx)
                    : p.userId();
            };
        }

        static KeyResolver byApiKey() {
            return ctx -> {
                Slice key = ctx.headerRaw("X-Api-Key");
                return (key != null && key.length() > 0)
                    ? key.materialize()
                    : byIp().resolve(ctx);
            };
        }

        static KeyResolver byHeader(String headerName) {
            return ctx -> {
                Slice val = ctx.headerRaw(headerName);
                return (val != null && val.length() > 0)
                    ? val.materialize()
                    : byIp().resolve(ctx);
            };
        }

        static KeyResolver composite(String separator, KeyResolver... resolvers) {
            return ctx -> {
                StringBuilder sb = new StringBuilder(64);

                int length = resolvers.length;
                for (int i = 0; i < length; i++) {
                    sb.append(resolvers[i].resolve(ctx));
                    if (i < length - 1) sb.append(separator);
                }

                return sb.toString();
            };
        }

        private static Slice firstToken(Slice s, char delimiter) {
            int len = s.length();

            for (int i = 0; i < len; i++) {
                if (s.charAt(i) == delimiter) {
                    return s.slice(0, i);
                }
            }

            return s;
        }

        private static Slice trim(Slice s) {
            int start = 0;
            int end = s.length();

            while (start < end && isSpace(s.charAt(start))) start++;
            while (end > start && isSpace(s.charAt(end - 1))) end--;

            return s.slice(start, end);
        }

        private static boolean isSpace(char c) {
            return c == ' ' || c == '\t' || c == '\n' || c == '\r';
        }
    }

    private final RateLimiter limiter;
    private final KeyResolver keyResolver;
    private final int statusCode;

    public RateLimiterFilter(RateLimiter limiter, KeyResolver keyResolver) {
        this(limiter, keyResolver, 429);
    }

    public RateLimiterFilter(RateLimiter limiter, KeyResolver keyResolver, int statusCode) {
        this.limiter = limiter;
        this.keyResolver = keyResolver;
        this.statusCode = statusCode;
    }

    @Override
    public ResponseEntity<?> handle(RequestContext ctx, FilterChain chain) {
        String key = keyResolver.resolve(ctx);
        RateLimitResult result = limiter.check(key);

        if (!result.allowed()) {
            return ResponseEntity.of(statusCode, new ErrorResponse(
                    "rate_limit_exceeded",
                    "Too many requests. Retry after " + result.resetAfterSeconds() + "s."
                ))
                .header("X-RateLimit-Limit",     Slice.of(result.limit()))
                .header("X-RateLimit-Remaining", SLICE_ZERO)
                .header("X-RateLimit-Reset",     Slice.of(result.resetAfterSeconds()))
                .header("Retry-After",           Slice.of(result.resetAfterSeconds()));
        }

        ResponseEntity<?> response = chain.next(ctx);

        return response.headers(headers -> {
            headers.set("X-RateLimit-Limit",     Slice.of(result.limit()));
            headers.set("X-RateLimit-Remaining", Slice.of(result.remaining()));
            headers.set("X-RateLimit-Reset",     Slice.of(result.resetAfterSeconds()));
        });
    }
}