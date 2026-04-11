package net.magnesiumbackend.transport.undertow.adapter;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import net.magnesiumbackend.core.http.HttpResponseAdapter;

import java.io.IOException;

public class UndertowResponseAdapter implements HttpResponseAdapter {

    private final HttpServerExchange exchange;

    public UndertowResponseAdapter(HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void setStatus(int statusCode) {
        exchange.setStatusCode(statusCode);
    }

    @Override
    public void setHeader(String name, String value) {
        exchange.getResponseHeaders().put(new HttpString(name), value);
    }

    @Override
    public void write(byte[] body) throws IOException {
        exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, String.valueOf(body.length));
        exchange.getOutputStream().write(body);
    }

    @Override
    public void write(byte[] body, int offset, int length) throws IOException {
        exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, String.valueOf(length));
        exchange.getOutputStream().write(body, offset, length);
    }
}