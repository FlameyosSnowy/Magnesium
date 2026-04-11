package net.magnesiumbackend.core.http.messages;

import net.magnesiumbackend.core.http.HttpResponseAdapter;
import net.magnesiumbackend.core.json.JsonProvider;

import java.io.IOException;

public final class JsonMessageConverter implements MessageConverter {

    private final JsonProvider json;

    public JsonMessageConverter(JsonProvider json) {
        this.json = json;
    }

    @Override
    public boolean canWrite(Class<?> type, String contentType) {
        return contentType.equals("application/json");
    }

    @Override
    public void write(Object body, HttpResponseAdapter adapter) throws IOException {
        byte[] bytes = json.toJsonBytes(body);
        adapter.write(bytes);
    }

    @Override
    public byte[] toBytes(Object body) {
        return json.toJsonBytes(body);
    }

    @Override
    public String contentType() {
        return "application/json";
    }
}