package net.magnesiumbackend.json.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.json.JsonProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Jackson implementation of {@link JsonProvider}.
 *
 * <p>Flexible JSON serialization using the Jackson library with extensive
 * customization options for type handling and property mapping.</p>
 */
public record JacksonJsonProvider(ObjectMapper objectMapper) implements JsonProvider {

    public JacksonJsonProvider() {
        this(new ObjectMapper()
            .findAndRegisterModules());
    }

    @Override
    public byte[] toJsonBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Jackson serialization failed", e);
        }
    }

    @Override
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Jackson serialization failed", e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Jackson deserialization failed", e);
        }
    }

    public <T> T fromJson(byte[] bytes, Class<T> type) {
        try {
            return objectMapper.readValue(bytes, type);
        } catch (IOException e) {
            throw new RuntimeException("Jackson deserialization failed", e);
        }
    }

    @Override
    public <T> T fromRequest(Request request, Class<T> type) {
        return fromJson(request.body(), type);
    }

    public <T> T fromJson(ByteArrayInputStream stream, Class<T> type) {
        try {
            return objectMapper.readValue(stream, type);
        } catch (IOException e) {
            throw new RuntimeException("Jackson stream deserialization failed", e);
        }
    }
}
