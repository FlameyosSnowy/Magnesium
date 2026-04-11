package net.magnesiumbackend.core.json;

import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/**
 * Strategy interface for JSON serialisation and deserialisation.
 *
 * <p>Implement this interface to plug in any JSON library (Jackson, Gson, etc.).
 * A single instance is registered on {@code MagnesiumApplication} at build time
 * and used for all request body parsing and response body writing.
 *
 * <p>Example:
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .json(new JacksonJsonProvider())
 *     ...
 * }</pre>
 */
public interface JsonProvider {

    /**
     * Serialises {@code value} to a JSON string.
     *
     * @param value the object to serialize; must not be {@code null}
     * @return a valid JSON string representation of {@code value}
     * @throws JsonException if serialization fails
     */
    String toJson(Object value);

    /**
     * Serialises {@code value} to a JSON string.
     *
     * @param value the object to serialize; must not be {@code null}
     * @return a valid JSON string representation of {@code value}
     * @throws JsonException if serialization fails
     */
    byte[] toJsonBytes(Object value);

    /**
     * Deserializes a JSON string into an instance of {@code type}.
     *
     * @param json  the JSON string to parse; must not be {@code null}
     * @param type  the target type; must not be {@code null}
     * @param <T>   the target type parameter
     * @return a new instance of {@code type} populated from {@code json}
     * @throws JsonException if deserialization fails or the JSON is malformed
     */
    <T> T fromJson(String json, Class<T> type);

    /**
     * Deserializes the body of {@code request} into an instance of {@code type}.
     *
     * <p>Shorthand for {@code fromJson(request.body(), type)}.
     *
     * @param request the incoming request whose {@code body()} is the JSON source
     * @param type    the target type
     * @param <T>     the target type parameter
     * @return a new instance of {@code type} populated from the request body
     * @throws JsonException if deserialization fails or the body is malformed JSON
     */
    default <T> T fromRequest(Request request, Class<T> type) {
        return fromJson(request.body(), type);
    }

    /**
     * Serialises {@code value} to JSON and wraps it in a 200 OK {@link ResponseEntity}.
     *
     * <p>Shorthand for {@code Response.status(toJson(value))}.
     *
     * @param value the object to serialize; must not be {@code null}
     * @return a {@link ResponseEntity} with status 200 and a JSON body
     * @throws JsonException if serialization fails
     */
    default <T> ResponseEntity<byte[]> toResponse(T value) {
        return switch (value) {
            case null -> ResponseEntity.of(200);
            case String val -> ResponseEntity.ok(val.getBytes(StandardCharsets.UTF_8));
            case byte[] val -> ResponseEntity.ok(val);
            default -> ResponseEntity.ok(toJsonBytes(value));
        };
    }

    /**
     * Unchecked wrapper for JSON errors, keeping method signatures clean
     * while still allowing callers to catch a specific exception type.
     */
    class JsonException extends RuntimeException {
        public JsonException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}