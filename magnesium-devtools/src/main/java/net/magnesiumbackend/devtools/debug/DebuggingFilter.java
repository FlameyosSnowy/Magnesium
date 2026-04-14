package net.magnesiumbackend.devtools.debug;

import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;

/**
 * A filter that wraps another filter to capture debugging information.
 *
 * <p>Automatically records filter execution time, short-circuit behavior,
 * and any errors thrown.</p>
 */
public final class DebuggingFilter implements HttpFilter {
    private final HttpFilter delegate;
    private final String filterName;
    private final int position;

    public DebuggingFilter(HttpFilter delegate, String filterName, int position) {
        this.delegate = delegate;
        this.filterName = filterName;
        this.position = position;
    }

    @Override
    public Object handle(RequestContext request, FilterChain chain) {
        if (!MagnesiumDebugger.isEnabled()) {
            return delegate.handle(request, chain);
        }

        MagnesiumDebugger.filterStart(filterName, position);

        Object result;
        final boolean[] proceeded = { false };

        try {
            // Track if the chain was actually called
            FilterChain wrappedChain = requestCtx -> {
                proceeded[0] = true;
                return chain.next(requestCtx);
            };

            result = delegate.handle(request, wrappedChain);

            // If result is not null and chain wasn't proceeded, this is a short-circuit
            ResponseEntity<?> response = toResponseEntity(result);
            MagnesiumDebugger.filterEnd(proceeded[0], response);

            return result;

        } catch (Exception e) {
            MagnesiumDebugger.recordError(e);
            throw e;
        }
    }

    private ResponseEntity<?> toResponseEntity(Object result) {
        if (result == null) return null;
        if (result instanceof ResponseEntity<?> re) {
            return re;
        }
        // Other results would be wrapped by the framework
        return null;
    }

    public String filterName() {
        return filterName;
    }

    public HttpFilter delegate() {
        return delegate;
    }
}
