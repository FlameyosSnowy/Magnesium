package net.magnesiumbackend.core.http.response;

import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Serializes {@link ResponseEntity} objects to HTTP responses.
 *
 * <p>HttpResponseWriter handles the conversion of ResponseEntity objects to raw
 * HTTP bytes through the provided {@link HttpResponseAdapter}. It supports:
 * <ul>
 *   <li>Null bodies (empty response)</li>
 *   <li>Raw byte arrays (passthrough)</li>
 *   <li>Strings (UTF-8 encoded)</li>
 *   <li>Objects (via {@link MessageConverterRegistry})</li>
 * </ul>
 * </p>
 *
 * <p>Content-Type resolution follows this order:
 * <ol>
 *   <li>Explicit Content-Type header in ResponseEntity</li>
 *   <li>Default to {@code application/json}</li>
 * </ol>
 * </p>
 *
 * @see ResponseEntity
 * @see HttpResponseAdapter
 * @see MessageConverterRegistry
 */
public final class HttpResponseWriter {

    private static final byte[] EMPTY = new byte[0];
    private static final Slice CONTENT_TYPE = Slice.of("application/json");

    private final MessageConverterRegistry converterRegistry;

    /**
     * Creates a new response writer with the given converter registry.
     *
     * @param converterRegistry the registry for converting objects to bytes
     */
    public HttpResponseWriter(MessageConverterRegistry converterRegistry) {
        this.converterRegistry = converterRegistry;
    }

    /**
     * Writes a ResponseEntity to the adapter.
     *
     * @param response the response entity to write
     * @param adapter  the target adapter for writing
     * @throws IOException if writing fails
     */
    public void write(ResponseEntity<?> response, HttpResponseAdapter adapter) throws IOException {
        Object body = response.body();
        Slice contentType = resolveContentType(response.headers());

        adapter.setStatus(response.statusCode());

        HttpHeaderIndex headers = response.headers();
        if (!headers.isEmpty()) {
            HttpHeaderIndex.Iterator iterator = headers.iterator();
            while (iterator.next()) {
                adapter.setHeader(iterator.nameSlice().materialize(), iterator.valueSlice().materialize());
            }
        }

        if (response.headers().get("Content-Type") != null) {
            adapter.setHeader("Content-Type", contentType.materialize());
        }

        switch (body) {
            case null -> adapter.write(EMPTY);
            case byte[] bytes -> adapter.write(bytes);
            case String str -> adapter.write(str.getBytes(StandardCharsets.UTF_8));
            default -> converterRegistry.findWriter(body, contentType.materialize()).write(body, adapter);
        }
    }

    private Slice resolveContentType(@NotNull HttpHeaderIndex headers) {
        return headers.getOrDefault("Content-Type", CONTENT_TYPE);
    }

    public byte[] toBytes(ResponseEntity<?> responseEntity) {
        return converterRegistry.findWriter(responseEntity.body(), resolveContentType(responseEntity.headers()).materialize())
            .toBytes(responseEntity.body());
    }
}