package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.auth.Principal;
import net.magnesiumbackend.core.auth.exceptions.AuthenticationException;
import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.security.CorrelationIdFilter;
import net.magnesiumbackend.core.headers.Slice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class RequestContext {
    private final Request request;
    private final Map<String, Object> attributes = new HashMap<>();
    private Principal principal = Principal.anonymous();
    private Map<String, String> cookieIndex;

    public RequestContext(Request request) {
        this.request = request;
    }

    public Request request() {
        return request;
    }

    public Map<String, String> pathVariables() {
        return request.pathVariables();
    }

    public @Nullable Slice header(String name) {
        return request.header(name);
    }

    /** Returns the cookie value by name, or null if absent */
    public @Nullable String cookie(String name) {
        if (cookieIndex == null) {
            cookieIndex = request.cookies();
        }
        return cookieIndex.get(name);
    }

    /** Returns the query param by name, or null if absent */
    public @Nullable String queryParam(String name) {
        return request.queryParam(name);
    }

    public void set(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) attributes.get(key);
    }

    public @NotNull Principal principal() {
        return principal;
    }

    public void setPrincipal(@NotNull Principal principal) {
        this.principal = principal;
    }

    public @NotNull Principal requirePrincipal() {
        if (principal.isAnonymous()) {
            throw new AuthenticationException("Authentication required");
        }
        return principal;
    }

    public @Nullable Slice correlationId() {
        return (Slice) attributes.get(CorrelationIdFilter.CTX_KEY);
    }

    @Override
    public String toString() {
        return "RequestContext{request=" + request + ", principal=" + principal + ", attributes=" + attributes + '}';
    }
}