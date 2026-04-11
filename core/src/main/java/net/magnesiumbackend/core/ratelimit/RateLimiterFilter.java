package net.magnesiumbackend.core.ratelimit;

import net.magnesiumbackend.core.auth.Principal;
import net.magnesiumbackend.core.http.response.ErrorResponse;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;

public final class RateLimiterFilter implements HttpFilter {

    @FunctionalInterface
    public interface KeyResolver {
        String resolve(RequestContext ctx);

        static KeyResolver byIp() {
            return ctx -> {
                Slice xff = ctx.header("X-Forwarded-For");

                if (xff != null && xff.length() > 0) {
                    Slice first = firstToken(xff, ',');
                    Slice trimmed = trim(first);
                    return trimmed.materialize();
                }

                Slice realIp = ctx.header("X-Real-IP");
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
                Slice key = ctx.header("X-Api-Key");
                return (key != null && key.length() > 0)
                    ? key.materialize()
                    : byIp().resolve(ctx);
            };
        }

        static KeyResolver byHeader(String headerName) {
            return ctx -> {
                Slice val = ctx.header(headerName);
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
                .header("X-RateLimit-Limit",     String.valueOf(result.limit()))
                .header("X-RateLimit-Remaining", "0")
                .header("X-RateLimit-Reset",     String.valueOf(result.resetAfterSeconds()))
                .header("Retry-After",           String.valueOf(result.resetAfterSeconds()));
        }

        ResponseEntity<?> response = chain.next(ctx);

        return response.headers(headers -> {
            headers.put("X-RateLimit-Limit",     String.valueOf(result.limit()));
            headers.put("X-RateLimit-Remaining", String.valueOf(result.remaining()));
            headers.put("X-RateLimit-Reset",     String.valueOf(result.resetAfterSeconds()));
        });
    }
}