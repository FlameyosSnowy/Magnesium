package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.ResponseEntity;

@FunctionalInterface
public interface HttpFilter {
    ResponseEntity<?> handle(RequestContext request, FilterChain chain);
}