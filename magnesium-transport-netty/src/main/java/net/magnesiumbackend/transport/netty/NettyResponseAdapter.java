package net.magnesiumbackend.transport.netty;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.magnesiumbackend.core.http.HttpResponseAdapter;

import java.io.IOException;

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
    public void write(byte[] body, int offset, int length) {
        response.content().clear().writeBytes(body, offset, length);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, length);
    }
}