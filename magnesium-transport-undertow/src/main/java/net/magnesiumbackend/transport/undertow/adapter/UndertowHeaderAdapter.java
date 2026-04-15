package net.magnesiumbackend.transport.undertow.adapter;

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.utils.ByteBufBuilder;

import java.util.List;
import java.util.Map;

public final class UndertowHeaderAdapter {

    public static HttpHeaderIndex from(io.undertow.server.HttpServerExchange exchange) {
        HeaderMap requestHeaders = exchange.getRequestHeaders();
        try (ByteBufBuilder buf = ByteBufBuilder.acquire(requestHeaders.size() * 32)) {
            for (HeaderValues values : requestHeaders) {
                String name = values.getHeaderName().toString();
                for (String v : values) {
                    buf.append(name);
                    buf.append((byte) ':');
                    buf.append(v);
                    buf.append((byte) '\r');
                    buf.append((byte) '\n');
                }
            }
            return new HttpHeaderIndex(buf.copyAndRelease());
        }
    }

    public static HttpHeaderIndex from(WebSocketHttpExchange exchange) {
        Map<String, List<String>> requestHeaders = exchange.getRequestHeaders();
        try (ByteBufBuilder buf = ByteBufBuilder.acquire(requestHeaders.size() * 32)) {
            for (Map.Entry<String, List<String>> values : requestHeaders.entrySet()) {
                String name = values.getKey();
                for (String v : values.getValue()) {
                    buf.append(name);
                    buf.append((byte) ':');
                    buf.append(v);
                    buf.append((byte) '\r');
                    buf.append((byte) '\n');
                }
            }
            return new HttpHeaderIndex(buf.copyAndRelease());
        }
    }
}