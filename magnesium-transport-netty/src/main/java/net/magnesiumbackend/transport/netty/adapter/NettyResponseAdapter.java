package net.magnesiumbackend.transport.netty.adapter;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.http.response.HttpResponseAdapter;

import java.nio.ByteBuffer;

public class NettyResponseAdapter implements HttpResponseAdapter {

    private final FullHttpResponse response;

    public NettyResponseAdapter(FullHttpResponse response) {
        this.response = response;
    }

    @Override
    public void setStatus(int statusCode) {
        response.setStatus(HttpResponseStatus.valueOf(statusCode));
    }

    @Override
    public void setHeader(String name, String value) {
        response.headers().set(name, value);
    }

    @Override
    public void write(byte[] body) {
        response.content().clear().writeBytes(body);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, body.length);
    }

    @Override
    public void write(ByteBuffer buffer) {
        response.content().clear().writeBytes(buffer);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, buffer.remaining());
    }

    @Override
    public void write(byte[] body, int offset, int length) {
        response.content().clear().writeBytes(body, offset, length);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, length);
    }

    @Override
    public void setHeader(Slice name, Slice value) {
        response.headers().set(
            new AsciiString(name.src(), name.start(), name.length(), false),
            new AsciiString(value.src(), value.start(), value.length(), false)
        );
    }
}