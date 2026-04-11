package net.magnesiumbackend.core.json;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonWriter;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.runtime.Settings;
import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class DslJsonProvider implements JsonProvider {

    private final DslJson<Object> dslJson;

    public DslJsonProvider() {
        this.dslJson = new DslJson<>(Settings.withRuntime()
            .includeServiceLoader()
        );
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
            throw new JsonException("DSL-JSON serialization failed", e);
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
            throw new JsonException("DSL-JSON deserialization failed", e);
        }
    }

    @Override
    public <T> T fromRequest(Request request, Class<T> type) {
        return fromJson(request.body(), type);
    }

    public <T> T fromJson(ByteArrayInputStream stream, Class<T> type) {
        try {
            return dslJson.deserialize(type, stream);

        } catch (IOException e) {
            throw new JsonException("DSL-JSON stream deserialization failed", e);
        }
    }

    // ------------------------
    // RESPONSE OPTIMIZATION
    // ------------------------

    @Override
    public ResponseEntity<byte[]> toResponse(Object value) {
        byte[] bytes = toJsonBytes(value);

        return ResponseEntity
            .of(200, bytes, Map.of("Content-Type", "application/json"));
    }
}