package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.auth.Principal;
import net.magnesiumbackend.core.auth.exceptions.AuthenticationException;
import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.headers.CookieIndex;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.security.CorrelationIdFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class RequestContext {

    private final Request request;

    private final Map<String, Object> attributes = new HashMap<>();

    private Principal principal = Principal.anonymous();

    private CookieIndex cookies;

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

    public @Nullable String cookie(String name) {
        if (cookies == null) {
            cookies = new CookieIndex(request.header("cookie"));
            cookies.parseIfNeeded();
        }
        return cookies.getValue(name);
    }

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
        return "RequestContext{request=" + request +
            ", principal=" + principal +
            ", attributes=" + attributes + '}';
    }
}