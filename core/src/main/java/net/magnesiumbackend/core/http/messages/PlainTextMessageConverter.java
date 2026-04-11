package net.magnesiumbackend.core.http.messages;

import net.magnesiumbackend.core.http.HttpResponseAdapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PlainTextMessageConverter implements MessageConverter {

    private static final byte[] EMPTY = new byte[0];

    @Override
    public String contentType() {
        return "text/plain";
    }

    @Override
    public boolean canWrite(Class<?> type, String contentType) {
        return contentType == null || contentType.equals("text/plain")
            && (type == String.class || isPrimitiveOrWrapper(type));
    }

    @Override
    public void write(Object body, HttpResponseAdapter adapter) throws IOException {
        if (body == null) {
            adapter.write(EMPTY);
            return;
        }
        adapter.write(toBytes(body));
    }

    @Override
    public byte[] toBytes(Object body) {
        if (body instanceof String s) {
            return s.getBytes(StandardCharsets.UTF_8);
        }

        return String.valueOf(body).getBytes(StandardCharsets.UTF_8);
    }

    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive()
            || type == Boolean.class
            || type == Byte.class
            || type == Short.class
            || type == Integer.class
            || type == Long.class
            || type == Float.class
            || type == Double.class
            || type == Character.class;
    }
}