package net.magnesiumbackend.core.http.messages;

import net.magnesiumbackend.core.json.JsonProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MessageConverterRegistry {

    private final Map<String, List<MessageConverter>> byContentType = new HashMap<>();

    private final Map<CacheKey, MessageConverter> cache = new ConcurrentHashMap<>();

    MessageConverterRegistry() {}

    public static @NotNull MessageConverterRegistry withDefaults(@Nullable JsonProvider jsonProvider) {
        MessageConverterRegistry registry = new MessageConverterRegistry();

        registry.register(new PlainTextMessageConverter());

        if (jsonProvider != null) {
            registry.register(new JsonMessageConverter(jsonProvider));
        }

        return registry;
    }

    public MessageConverterRegistry register(MessageConverter converter) {
        byContentType
            .computeIfAbsent(converter.contentType(), k -> new ArrayList<>())
            .add(converter);
        return this;
    }

    public MessageConverter findWriter(Object body, String contentType) {
        if (body == null) {
            throw new IllegalArgumentException("body cannot be null");
        }

        Class<?> type = body.getClass();
        CacheKey key = new CacheKey(type, contentType);

        MessageConverter cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        List<MessageConverter> candidates = byContentType.get(contentType);

        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No converters for contentType=" + contentType);
        }

        for (MessageConverter converter : candidates) {
            if (converter.canWrite(type, contentType)) {
                cache.put(key, converter);
                return converter;
            }
        }

        throw new IllegalStateException(
            "No converter for type=" + type.getName() +
                ", contentType=" + contentType
        );
    }

    private record CacheKey(Class<?> type, String contentType) {}
}