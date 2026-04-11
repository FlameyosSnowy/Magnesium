package net.magnesiumbackend.core.http.response;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ResponseEntity<T> {

    int statusCode();
    @Nullable
    T body();
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