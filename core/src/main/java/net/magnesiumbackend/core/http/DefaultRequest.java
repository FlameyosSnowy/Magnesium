package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.Slice;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record DefaultRequest(
    String path,
    String body,
    HttpVersion version,
    HttpMethod method,
    Map<String, String> queryParams,
    Map<String, String> pathVariables,
    RouteDefinition routeDefinition,
    HttpHeaderIndex headerIndex
) implements Request {

    @Override
    public @Nullable Slice header(String name) {
        return headerIndex.get(name);
    }
}