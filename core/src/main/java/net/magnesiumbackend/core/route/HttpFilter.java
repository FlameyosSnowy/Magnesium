package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.response.ResponseEntity;

@FunctionalInterface
public interface HttpFilter {
    ResponseEntity<?> handle(RequestContext request, FilterChain chain);
}