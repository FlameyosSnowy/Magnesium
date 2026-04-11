package net.magnesiumbackend.transport.tomcat;

import net.magnesiumbackend.core.http.HttpResponseAdapter;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TomcatResponseAdapter implements HttpResponseAdapter {

    private final HttpServletResponse response;

    public TomcatResponseAdapter(HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public void setStatus(int statusCode) {
        response.setStatus(statusCode);
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    @Override
    public void write(byte[] body) throws IOException {
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    @Override
    public void write(byte[] body, int offset, int length) throws IOException {
        response.setContentLength(length);
        response.getOutputStream().write(body, offset, length);
    }
}