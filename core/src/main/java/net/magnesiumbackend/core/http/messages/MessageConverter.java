package net.magnesiumbackend.core.http.messages;

import net.magnesiumbackend.core.http.response.HttpResponseAdapter;

import java.io.IOException;

public interface MessageConverter {

    boolean canWrite(Class<?> type, String contentType);

    /**
     * Write directly to the response (preferred path).
     */
    void write(Object body, HttpResponseAdapter adapter) throws IOException;

    /**
     * Optional fast-path for transports that support direct buffers (e.g. Netty).
     *
     * Default = fallback to write()
     */
    default void writeDirect(Object body, HttpResponseAdapter adapter) throws IOException {
        write(body, adapter);
    }

    /**
     * Optional legacy fallback.
     * Avoid using this in hot paths.
     */
    default byte[] toBytes(Object body) {
        throw new UnsupportedOperationException("toBytes not supported");
    }

    String contentType();
}