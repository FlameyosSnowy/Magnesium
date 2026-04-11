package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.ResponseEntity;

public interface FilterChain {
    ResponseEntity<?> next(RequestContext ctx);
}