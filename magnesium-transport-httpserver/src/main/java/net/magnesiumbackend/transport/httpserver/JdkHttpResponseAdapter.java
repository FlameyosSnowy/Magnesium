package net.magnesiumbackend.transport.httpserver;

import com.sun.net.httpserver.HttpExchange;
import net.magnesiumbackend.core.http.HttpResponseAdapter;

import java.io.IOException;
import java.io.OutputStream;

public class JdkHttpResponseAdapter implements HttpResponseAdapter {

    private final HttpExchange exchange;

    public JdkHttpResponseAdapter(HttpExchange exchange) {
        this.exchange = exchange;
    }

    private int statusCode = 200;

    @Override
    public void setStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void write(byte[] body) throws IOException {
        exchange.sendResponseHeaders(statusCode, body.length);
        exchange.getResponseBody().write(body);
    }

    @Override
    public void write(byte[] body, int offset, int length) throws IOException {
        exchange.sendResponseHeaders(statusCode, length);
        exchange.getResponseBody().write(body, offset, length);
    }

    @Override
    public void setHeader(String name, String value) {
        exchange.getResponseHeaders().set(name, value);
    }
}