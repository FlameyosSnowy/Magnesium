package net.magnesiumbackend.transport.tomcat.adapter;

import jakarta.servlet.http.HttpServletRequest;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.utils.ByteBufBuilder;

import java.util.Enumeration;

public final class TomcatHeaderAdapter {

    public static HttpHeaderIndex from(HttpServletRequest req) {
        try (ByteBufBuilder buf = ByteBufBuilder.acquire(256)) {
            Enumeration<String> names = req.getHeaderNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                Enumeration<String> values = req.getHeaders(name);
                while (values.hasMoreElements()) {
                    buf.append(name);
                    buf.append((byte) ':');
                    buf.append(values.nextElement());
                    buf.append((byte) '\r');
                    buf.append((byte) '\n');
                }
            }
            return new HttpHeaderIndex(buf.copyAndRelease());
        }
    }
}