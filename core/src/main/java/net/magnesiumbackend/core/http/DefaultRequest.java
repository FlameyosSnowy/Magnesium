package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.headers.HttpQueryParamIndex;
import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.HttpVersion;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.Slice;
import org.jetbrains.annotations.Nullable;

public record DefaultRequest(
    String path,
    byte[] bodyAsBytes,
    HttpVersion version,
    HttpMethod method,
    HttpQueryParamIndex queryParams,
    HttpPathParamIndex pathVariables,
    RouteDefinition routeDefinition,
    HttpHeaderIndex headerIndex
) implements Request {

    @Override
    public @Nullable Slice header(String name) {
        return headerIndex.get(name);
    }
}