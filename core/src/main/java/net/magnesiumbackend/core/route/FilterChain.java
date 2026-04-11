package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.response.ResponseEntity;

public interface FilterChain {
    ResponseEntity<?> next(RequestContext ctx);
}