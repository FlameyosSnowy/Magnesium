package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.http.messages.MessageConverter;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpResponseWriter {

    private static final byte[] EMPTY = new byte[0];

    private final MessageConverterRegistry converterRegistry;

    public HttpResponseWriter(MessageConverterRegistry converterRegistry) {
        this.converterRegistry = converterRegistry;
    }

    public void write(ResponseEntity<?> response, HttpResponseAdapter adapter) throws IOException {
        Object body = response.body();
        String contentType = resolveContentType(response.headers());

        adapter.setStatus(response.statusCode());

        Map<String, String> headers = response.headers();
        if (!headers.isEmpty()) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                adapter.setHeader(e.getKey(), e.getValue());
            }
        }

        if (!response.headers().containsKey("Content-Type")) {
            adapter.setHeader("Content-Type", contentType);
        }

        switch (body) {
            case null -> adapter.write(EMPTY);
            case byte[] bytes -> adapter.write(bytes);
            case String str -> adapter.write(str.getBytes(StandardCharsets.UTF_8));
            default -> converterRegistry.findWriter(body, contentType).write(body, adapter);
        }
    }



    private String resolveContentType(@NotNull Map<String, String> headers) {
        return headers.getOrDefault("Content-Type", "application/json");
    }

    public byte[] toBytes(ResponseEntity<?> responseEntity) {
        return this.converterRegistry.findWriter(responseEntity.body(), resolveContentType(responseEntity.headers()))
            .toBytes(responseEntity.body());
    }
}