package net.magnesiumbackend.core.http.messages;

import net.magnesiumbackend.core.http.response.HttpResponseAdapter;
import net.magnesiumbackend.core.http.response.HttpResponseWriter;

import java.io.IOException;

/**
 * Converts response body objects to HTTP response bytes.
 *
 * <p>MessageConverters handle the serialization of Java objects to various
 * content types (JSON, XML, plain text, etc.). The framework uses the
 * converter registry to select the appropriate converter based on the
 * object's class and the requested content type.</p>
 *
 * <h3>Implementations</h3>
 * <ul>
 *   <li>{@link JsonMessageConverter} - JSON serialization using DslJson</li>
 *   <li>{@link PlainTextMessageConverter} - Plain text for String bodies</li>
 * </ul>
 *
 * @see MessageConverterRegistry
 * @see HttpResponseWriter
 */
public interface MessageConverter {

    /**
     * Checks if this converter can handle the given type and content type.
     *
     * @param type        the body object class
     * @param contentType the requested content type (e.g., "application/json")
     * @return true if this converter can serialize the object
     */
    boolean canWrite(Class<?> type, String contentType);

    /**
     * Writes the body object to the response adapter.
     *
     * @param body    the object to serialize
     * @param adapter the target adapter for writing
     * @throws IOException if writing fails
     */
    void write(Object body, HttpResponseAdapter adapter) throws IOException;

    /**
     * Converts the body object to a byte array.
     *
     * @param body the object to serialize
     * @return the serialized bytes
     */
    byte[] toBytes(Object body);

    /**
     * Returns the content type this converter produces.
     *
     * @return the MIME type (e.g., "application/json")
     */
    String contentType();
}