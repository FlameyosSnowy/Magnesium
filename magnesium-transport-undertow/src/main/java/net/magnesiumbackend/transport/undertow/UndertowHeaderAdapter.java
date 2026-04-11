package net.magnesiumbackend.transport.undertow;

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.utils.ByteBufBuilder;

public final class UndertowHeaderAdapter {

    public static HttpHeaderIndex from(io.undertow.server.HttpServerExchange exchange) {
        HeaderMap requestHeaders = exchange.getRequestHeaders();
        ByteBufBuilder buf = new ByteBufBuilder(requestHeaders.size() * 32);

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

        return new HttpHeaderIndex(buf.build());
    }
}