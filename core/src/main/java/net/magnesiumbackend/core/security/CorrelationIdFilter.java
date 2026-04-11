package net.magnesiumbackend.core.security;

import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.headers.Slice;
import org.slf4j.MDC;

import java.util.UUID;

public final class CorrelationIdFilter implements HttpFilter {

    public static final String HEADER  = "X-Request-Id";
    public static final String CTX_KEY = "__correlationId";
    public static final String MDC_KEY = "requestId";

    private final IdGenerator generator;

    @FunctionalInterface
    public interface IdGenerator {
        String generate();
    }

    public CorrelationIdFilter() {
        this(() -> UUID.randomUUID().toString().replace("-", ""));
    }

    public CorrelationIdFilter(IdGenerator generator) {
        this.generator = generator;
    }

    @Override
    public ResponseEntity<?> handle(RequestContext ctx, FilterChain chain) {

        Slice id = ctx.header(HEADER);

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