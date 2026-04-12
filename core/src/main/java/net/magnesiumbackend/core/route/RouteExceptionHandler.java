package net.magnesiumbackend.core.route;

@FunctionalInterface
public interface RouteExceptionHandler {
    Object handle(Throwable error, RequestContext ctx);
}