package net.magnesiumbackend.core.http.response;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Represents an HTTP response with status code, body, and headers.
 *
 * <p>ResponseEntity is the standard return type for controller methods in Magnesium.
 * It provides a fluent API for building responses with various status codes,
 * headers, and body content.</p>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Simple 200 OK with body
 * return ResponseEntity.ok(user);
 *
 * // 201 Created
 * return ResponseEntity.created(newUser);
 *
 * // 204 No Content
 * return ResponseEntity.noContent();
 *
 * // Custom status with headers
 * return ResponseEntity.of(200, data)
 *     .header("X-Total-Count", String.valueOf(total))
 *     .header("Cache-Control", "max-age=3600");
 *
 * // Error response
 * return ResponseEntity.of(404, Map.of("error", "User not found"));
 * }</pre>
 *
 * @param <T> the type of the response body
 * @see DefaultResponseEntity
 */
public interface ResponseEntity<T> {

    /**
     * Returns the HTTP status code.
     *
     * @return the status code (e.g., 200, 404)
     */
    int statusCode();

    /**
     * Returns the response body, or null if none.
     *
     * @return the body object, or null
     */
    @Nullable
    T body();

    /**
     * Returns the mutable map of response headers.
     *
     * @return the headers map (modifiable)
     */
    Map<String, String> headers();

    default ResponseEntity<T> headers(Map<String, String> headers) {
        headers().putAll(headers);
        return this;
    }

    default ResponseEntity<T> headers(BiConsumer<String, String> headers) {
        headers().forEach(headers);
        return this;
    }

    default ResponseEntity<T> headers(Consumer<Map<String, String>> headers) {
        headers.accept(headers());
        return this;
    }

    default ResponseEntity<T> header(String name, String value) {
        headers().put(name, value);
        return this;
    }

    static <T> ResponseEntity<T> of(int statusCode, T body, Map<String, String> headers) {
        return new DefaultResponseEntity<>(statusCode, body, headers);
    }

    static <T> ResponseEntity<T> of(int statusCode, T body) {
        return new DefaultResponseEntity<>(statusCode, body, Collections.emptyMap());
    }

    static <T> ResponseEntity<T> of(int statusCode) {
        return new DefaultResponseEntity<>(statusCode, null, Collections.emptyMap());
    }

    static <T> ResponseEntity<T> ok(T body) {
        return of(200, body);
    }

    static ResponseEntity<Void> ok() {
        return of(200, null);
    }

    static <T> ResponseEntity<T> created(T body) {
        return of(201, body);
    }

    static ResponseEntity<Void> noContent() {
        return of(204, null);
    }
}