package net.magnesiumbackend.core.http;

import java.io.IOException;

public interface HttpResponseAdapter {

    void setStatus(int statusCode);

    void setHeader(String name, String value);

    void write(byte[] body) throws IOException;

    void write(byte[] bytes, int offset, int length) throws IOException;
}