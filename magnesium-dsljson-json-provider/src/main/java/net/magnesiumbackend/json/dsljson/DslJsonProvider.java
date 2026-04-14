package net.magnesiumbackend.json.dsljson;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonWriter;
import com.dslplatform.json.runtime.Settings;
import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.json.JsonProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * DSL-JSON implementation of {@link JsonProvider}.
 *
 * <p>High-performance JSON serialization using the DSL-JSON library.</p>
 */
public final class DslJsonProvider implements JsonProvider {

    private final DslJson<Object> dslJson;

    public DslJsonProvider() {
        this.dslJson = new DslJson<>(Settings.withRuntime()
            .includeServiceLoader());
    }

    public DslJsonProvider(DslJson<Object> dslJson) {
        this.dslJson = dslJson;
    }

    @Override
    public byte[] toJsonBytes(Object value) {
        try {
            JsonWriter writer = dslJson.newWriter();
            dslJson.serialize(writer, value);

            byte[] result = new byte[writer.size()];
            System.arraycopy(writer.getByteBuffer(), 0, result, 0, writer.size());
            return result;

        } catch (IOException e) {
            throw new RuntimeException("DSL-JSON serialization failed", e);
        }
    }

    @Override
    public String toJson(Object value) {
        return new String(toJsonBytes(value), StandardCharsets.UTF_8);
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return fromJson(bytes, type);
    }

    public <T> T fromJson(byte[] bytes, Class<T> type) {
        try {
            return dslJson.deserialize(type, bytes, bytes.length);

        } catch (IOException e) {
            throw new RuntimeException("DSL-JSON deserialization failed", e);
        }
    }

    @Override
    public <T> T fromRequest(Request request, Class<T> type) {
        return fromJson(request.bodyAsBytes(), type);
    }

    public <T> T fromJson(ByteArrayInputStream stream, Class<T> type) {
        try {
            return dslJson.deserialize(type, stream);

        } catch (IOException e) {
            throw new RuntimeException("DSL-JSON stream deserialization failed", e);
        }
    }

    @Override
    public Map<String, Object> deserializeToMap(InputStream in) {
        try {
            Object result = dslJson.deserialize(Object.class, in);
            if (result instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedMap = (Map<String, Object>) map;
                return typedMap;
            }
            throw new JsonException("JSON root must be an object", null);
        } catch (IOException e) {
            throw new JsonException("DSL-JSON deserialization failed", e);
        }
    }
}
