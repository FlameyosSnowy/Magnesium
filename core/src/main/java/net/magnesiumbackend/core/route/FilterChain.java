package net.magnesiumbackend.core.route;

public interface FilterChain {
    Object next(RequestContext ctx);
}