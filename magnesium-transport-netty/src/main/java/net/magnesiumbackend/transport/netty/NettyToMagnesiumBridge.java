package net.magnesiumbackend.transport.netty;

import io.netty.handler.codec.http.HttpVersion;
import net.magnesiumbackend.core.http.HttpMethod;
import org.jetbrains.annotations.Contract;

import java.util.Objects;

public final class NettyToMagnesiumBridge {

    private NettyToMagnesiumBridge() {}

    @Contract(pure = true)
    public static io.netty.handler.codec.http.HttpMethod asNettyMethod(HttpMethod method) {
        Objects.requireNonNull(method, "HttpMethod must not be null.");
        return switch (method) {
            case GET     -> io.netty.handler.codec.http.HttpMethod.GET;
            case POST    -> io.netty.handler.codec.http.HttpMethod.POST;
            case PUT     -> io.netty.handler.codec.http.HttpMethod.PUT;
            case PATCH   -> io.netty.handler.codec.http.HttpMethod.PATCH;
            case DELETE  -> io.netty.handler.codec.http.HttpMethod.DELETE;
            case HEAD    -> io.netty.handler.codec.http.HttpMethod.HEAD;
            case TRACE   -> io.netty.handler.codec.http.HttpMethod.TRACE;
            case CONNECT -> io.netty.handler.codec.http.HttpMethod.CONNECT;
            case OPTIONS -> io.netty.handler.codec.http.HttpMethod.OPTIONS;
        };
    }

    @Contract(pure = true)
    public static HttpMethod asMagnesiumMethod(io.netty.handler.codec.http.HttpMethod method) {
        Objects.requireNonNull(method, "Netty HttpMethod must not be null.");
        if (method == io.netty.handler.codec.http.HttpMethod.GET)     return HttpMethod.GET;
        if (method == io.netty.handler.codec.http.HttpMethod.POST)    return HttpMethod.POST;
        if (method == io.netty.handler.codec.http.HttpMethod.PUT)     return HttpMethod.PUT;
        if (method == io.netty.handler.codec.http.HttpMethod.PATCH)   return HttpMethod.PATCH;
        if (method == io.netty.handler.codec.http.HttpMethod.DELETE)  return HttpMethod.DELETE;
        if (method == io.netty.handler.codec.http.HttpMethod.HEAD)    return HttpMethod.HEAD;
        if (method == io.netty.handler.codec.http.HttpMethod.TRACE)   return HttpMethod.TRACE;
        if (method == io.netty.handler.codec.http.HttpMethod.CONNECT) return HttpMethod.CONNECT;
        return HttpMethod.OPTIONS;
    }

    /**
     * Converts a raw HTTP/2 method string (e.g. from Http2Headers.method())
     * into a Magnesium HttpMethod.
     */
    @Contract(pure = true)
    public static HttpMethod asMagnesiumMethodFromString(String method) {
        Objects.requireNonNull(method, "Method string must not be null.");
        return asMagnesiumMethod(
            io.netty.handler.codec.http.HttpMethod.valueOf(method.toUpperCase())
        );
    }

    @Contract(pure = true)
    public static HttpVersion asNettyVersion(net.magnesiumbackend.core.http.HttpVersion version) {
        Objects.requireNonNull(version, "HttpVersion must not be null.");
        return switch (version) {
            case HTTP_1_0 -> HttpVersion.HTTP_1_0;
            case HTTP_1_1 -> HttpVersion.HTTP_1_1;
            // HTTP/2 uses a separate frame codec — not representable as HttpVersion
            case HTTP_2_0 -> throw new UnsupportedOperationException(
                "HTTP/2 uses a separate codec pipeline, not HttpVersion."
            );
        };
    }

    @Contract(pure = true)
    public static net.magnesiumbackend.core.http.HttpVersion asMagnesiumVersion(HttpVersion version) {
        Objects.requireNonNull(version, "Netty HttpVersion must not be null.");
        if (version == HttpVersion.HTTP_1_0) return net.magnesiumbackend.core.http.HttpVersion.HTTP_1_0;
        if (version == HttpVersion.HTTP_1_1) return net.magnesiumbackend.core.http.HttpVersion.HTTP_1_1;
        throw new IllegalArgumentException("Unsupported Netty HttpVersion: " + version);
    }

    /**
     * HTTP/2 frames carry no HttpVersion — version is implied by the codec.
     * Use this instead of asMagnesiumVersion() in Http2ServerHandler.
     */
    @Contract(pure = true)
    public static net.magnesiumbackend.core.http.HttpVersion http2Version() {
        return net.magnesiumbackend.core.http.HttpVersion.HTTP_2_0;
    }
}
