package net.magnesiumbackend.transport.netty.adapter;

import io.netty.handler.codec.http.HttpHeaders;

import io.netty.handler.codec.http2.Http2Headers;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.utils.ByteBufBuilder;

import java.util.Map;

public final class NettyHeaderAdapter {

    public static HttpHeaderIndex from(HttpHeaders headers) {

        ByteBufBuilder buf = new ByteBufBuilder(headers.size() * 32);

        for (Map.Entry<String, String> e : headers) {
            buf.append(e.getKey());
            buf.append((byte) ':');
            buf.append(e.getValue());
            buf.append((byte) '\r');
            buf.append((byte) '\n');
        }

        return new HttpHeaderIndex(buf.build());
    }

    public static HttpHeaderIndex from(Http2Headers headers) {

        ByteBufBuilder buf = new ByteBufBuilder(headers.size() * 32);

        for (Map.Entry<CharSequence, CharSequence> e : headers) {
            buf.append(e.getKey().toString());
            buf.append((byte) ':');
            buf.append(e.getValue().toString());
            buf.append((byte) '\r');
            buf.append((byte) '\n');
        }

        return new HttpHeaderIndex(buf.build());
    }
}