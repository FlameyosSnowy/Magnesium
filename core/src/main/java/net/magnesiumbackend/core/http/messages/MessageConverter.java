package net.magnesiumbackend.core.http.messages;

import net.magnesiumbackend.core.http.HttpResponseAdapter;

import java.io.IOException;

public interface MessageConverter {

    boolean canWrite(Class<?> type, String contentType);

    void write(Object body, HttpResponseAdapter adapter) throws IOException;

    byte[] toBytes(Object body);

    String contentType();
}