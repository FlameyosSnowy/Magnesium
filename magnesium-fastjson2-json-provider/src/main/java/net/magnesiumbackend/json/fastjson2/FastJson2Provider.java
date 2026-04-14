package net.magnesiumbackend.json.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.json.JsonProvider;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * FastJSON2 implementation of {@link JsonProvider}.
 *
 * <p>High-performance JSON serialization optimized for throughput.</p>
 */
public final class FastJson2Provider implements JsonProvider {

    private final JSONWriter.Feature[] writerFeatures;
    private final JSONReader.Feature[] readerFeatures;

    public FastJson2Provider() {
        this.writerFeatures = new JSONWriter.Feature[] {
            JSONWriter.Feature.FieldBased
        };
        this.readerFeatures = new JSONReader.Feature[] {
            JSONReader.Feature.FieldBased
        };
    }

    public FastJson2Provider(JSONWriter.Feature[] writerFeatures,
                             JSONReader.Feature[] readerFeatures) {
        this.writerFeatures = writerFeatures;
        this.readerFeatures = readerFeatures;
    }

    @Override
    public byte[] toJsonBytes(Object value) {
        return JSON.toJSONBytes(value, writerFeatures);
    }

    @Override
    public String toJson(Object value) {
        return JSON.toJSONString(value, writerFeatures);
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        return JSON.parseObject(json, type, readerFeatures);
    }

    public <T> T fromJson(byte[] bytes, Class<T> type) {
        return JSON.parseObject(bytes, type, readerFeatures);
    }

    @Override
    public <T> T fromRequest(Request request, Class<T> type) {
        return fromJson(request.body(), type);
    }

    public <T> T fromJson(ByteArrayInputStream stream, Class<T> type) {
        byte[] bytes;
        try {
            bytes = stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("FastJSON2 stream read failed", e);
        }
        return fromJson(bytes, type);
    }
}
