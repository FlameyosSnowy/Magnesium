package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.ResponseEntity;

@FunctionalInterface
public interface RouteExceptionHandler {
    ResponseEntity handle(Throwable error, RequestContext ctx);
}