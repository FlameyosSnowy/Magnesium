package net.magnesiumbackend.core.security;

import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.headers.Slice;
import org.slf4j.MDC;

import java.util.Objects;
import java.util.UUID;

/**
 * HTTP filter that manages request correlation IDs for distributed tracing.
 *
 * <p>CorrelationIdFilter ensures every request has a unique identifier that
 * flows through the system for logging and debugging purposes. It:
 * <ul>
 *   <li>Reads existing X-Request-Id header if present</li>
 *   <li>Generates a new UUID if no correlation ID exists</li>
 *   <li>Stores the ID in RequestContext for access by handlers</li>
 *   <li>Puts the ID in MDC for logging frameworks</li>
 * </ul>
 * </p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .filter(new CorrelationIdFilter())
 *     .build();
 *
 * // In log patterns: %X{requestId}
 * // Access in handler: ctx.get(CorrelationIdFilter.CTX_KEY)
 * }</pre>
 *
 * @see HttpFilter
 * @see MDC
 */
public final class CorrelationIdFilter implements HttpFilter {

    /** Header name for the correlation ID. */
    public static final String HEADER  = "X-Request-Id";

    /** RequestContext key for accessing the correlation ID. */
    public static final String CTX_KEY = "__correlationId";

    /** MDC key for logging correlation ID. */
    public static final String MDC_KEY = "requestId";

    private final IdGenerator generator;

    /**
     * Functional interface for generating correlation IDs.
     */
    @FunctionalInterface
    public interface IdGenerator {
        /**
         * Generates a new correlation ID string.
         *
         * @return the generated ID
         */
        String generate();
    }

    /**
     * Creates a filter with default UUID-based ID generation.
     */
    public CorrelationIdFilter() {
        this(() -> UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * Creates a filter with a custom ID generator.
     *
     * @param generator the ID generator to use
     */
    public CorrelationIdFilter(IdGenerator generator) {
        this.generator = generator;
    }

    @Override
    public Object handle(RequestContext ctx, FilterChain chain) {

        Slice id = ctx.headerRaw(HEADER);

        if (id == null || id.len() == 0) {
            String generated = generator.generate();
            id = new Slice(generated.getBytes(), 0, generated.length());
        }

        ctx.set(CTX_KEY, id);

        // Only boundary conversion here (correct)
        MDC.put(MDC_KEY, id.materialize());

        try {
            return chain.next(ctx);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}