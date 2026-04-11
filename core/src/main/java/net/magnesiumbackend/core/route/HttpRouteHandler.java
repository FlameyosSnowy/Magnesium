package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.response.ResponseEntity;

@FunctionalInterface
public interface HttpRouteHandler {
    ResponseEntity handle(RequestContext request);
}