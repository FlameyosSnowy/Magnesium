package net.magnesiumbackend.transport.httpserver;

import com.sun.net.httpserver.Headers;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.utils.ByteBufBuilder;

import java.util.List;
import java.util.Map;

public final class JdkHeaderAdapter {

    public static HttpHeaderIndex from(Headers headers) {

        ByteBufBuilder buf = new ByteBufBuilder(headers.size() * 32);

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();

            for (String value : entry.getValue()) {
                buf.append(name);
                buf.append((byte) ':');
                buf.append(value);
                buf.append((byte) '\r');
                buf.append((byte) '\n');
            }
        }

        return new HttpHeaderIndex(buf.build());
    }
}