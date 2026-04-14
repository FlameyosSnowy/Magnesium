package net.magnesiumbackend.core.http.messages;

import net.magnesiumbackend.core.http.response.HttpResponseAdapter;
import net.magnesiumbackend.core.json.JsonProvider;

import java.io.IOException;

public final class JsonMessageConverter implements MessageConverter {

    private static final String CONTENT_TYPE = "application/json";

    private final JsonProvider json;

    public JsonMessageConverter(JsonProvider json) {
        this.json = json;
    }

    @Override
    public boolean canWrite(Class<?> type, String contentType) {
        return CONTENT_TYPE.equals(contentType);
    }

    @Override
    public void write(Object body, HttpResponseAdapter adapter) throws IOException {
        if (body instanceof byte[] bytes) {
            adapter.write(bytes);
            return;
        }

        adapter.write(json.toJsonBytes(body));
    }

    @Override
    public byte[] toBytes(Object body) {
        if (body instanceof byte[] bytes) {
            return bytes;
        }

        return json.toJsonBytes(body);
    }

    @Override
    public String contentType() {
        return CONTENT_TYPE;
    }
}