package net.magnesiumbackend.transport.netty.adapter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AsciiString;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class NettyHeaderAdapter {

    public static HttpHeaderIndex from(HttpHeaders headers, ByteBufAllocator alloc) {

        ByteBuf buf = alloc.buffer(headers.size() * 32);

        try {
            for (Map.Entry<String, String> e : headers) {
                append(buf, e.getKey());
                buf.writeByte(':');
                append(buf, e.getValue());
                buf.writeByte('\r');
                buf.writeByte('\n');
            }

            byte[] copy = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), copy);

            return new HttpHeaderIndex(copy);

        } finally {
            buf.release();
        }
    }

    public static HttpHeaderIndex from(Http2Headers headers, ByteBufAllocator alloc) {

        ByteBuf buf = alloc.buffer(headers.size() * 32);

        try {
            for (Map.Entry<CharSequence, CharSequence> e : headers) {
                append(buf, e.getKey());
                buf.writeByte(':');
                append(buf, e.getValue());
                buf.writeByte('\r');
                buf.writeByte('\n');
            }

            byte[] copy = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), copy);

            return new HttpHeaderIndex(copy);

        } finally {
            buf.release();
        }
    }

    private static void append(ByteBuf buf, CharSequence cs) {

        if (cs instanceof AsciiString ascii) {
            buf.writeBytes(ascii.array(), ascii.arrayOffset(), ascii.length());
            return;
        }

        if (cs instanceof String s) {
            buf.writeCharSequence(s, StandardCharsets.US_ASCII);
            return;
        }

        int len = cs.length();
        for (int i = 0; i < len; i++) {
            buf.writeByte((byte) cs.charAt(i));
        }
    }
}