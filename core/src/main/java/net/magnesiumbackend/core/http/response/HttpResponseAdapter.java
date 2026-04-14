package net.magnesiumbackend.core.http.response;

import net.magnesiumbackend.core.headers.Slice;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Abstraction for writing HTTP responses to the underlying transport.
 *
 * <p>HttpResponseAdapter decouples response writing from specific transport
 * implementations (Netty, Tomcat, etc.). Transports provide implementations
 * that write to their native response buffers.</p>
 *
 * <p>Used by {@link HttpResponseWriter} to serialize {@link ResponseEntity}
 * objects to the wire.</p>
 *
 * @see HttpResponseWriter
 * @see ResponseEntity
 */
public interface HttpResponseAdapter {

    /**
     * Sets the HTTP status code for the response.
     *
     * @param statusCode the HTTP status code (e.g., 200, 404)
     */
    void setStatus(int statusCode);

    /**
     * Sets a response header.
     *
     * @param name  the header name
     * @param value the header value
     */
    void setHeader(String name, String value);

    /**
     * Writes the response body bytes.
     *
     * @param body the body bytes to write
     * @throws IOException if writing fails
     */
    void write(byte[] body) throws IOException;

    /**
     * Writes a portion of a byte array as the response body.
     *
     * @param bytes  the source byte array
     * @param offset the starting offset
     * @param length the number of bytes to write
     * @throws IOException if writing fails
     */
    void write(byte[] bytes, int offset, int length) throws IOException;

    /**
     * Zero-copy capable write
     */
    default void write(ByteBuffer buffer) throws IOException {
        write(buffer.array()); // fallback
    }

    /**
     * Optional: streaming support (future-proof)
     */
    default void writeChunk(byte[] chunk, boolean last) throws IOException {
        write(chunk);
    }

    void setHeader(Slice key, Slice value);
}