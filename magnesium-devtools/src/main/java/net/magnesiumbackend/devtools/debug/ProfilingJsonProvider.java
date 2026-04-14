package net.magnesiumbackend.devtools.debug;

import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.json.JsonProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A JsonProvider wrapper that profiles serialization and deserialization operations.
 *
 * <p>Captures timing data for all JSON operations to identify performance bottlenecks
 * in request/response processing. All methods delegate to the underlying provider while
 * recording execution metrics.</p>
 *
 * <p>This provider is loaded via {@link ServiceLoader} and takes precedence over other
 * JsonProvider implementations. It automatically wraps another JsonProvider found on the
 * classpath via service loading. If no JsonProvider is found, a runtime exception is thrown.</p>
 *
 * <p>Service file location: {@code META-INF/services/net.magnesiumbackend.core.json.JsonProvider}</p>
 *
 * <p>To ensure ProfilingJsonProvider is loaded first, it should be listed first in the
 * service file, or use the devtools module which provides this service.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // Automatically loaded via ServiceLoader
 * JsonProvider provider = JsonProviderLoader.load()
 *     .orElseThrow(() -> new IllegalStateException("No JSON provider found"));
 * }</pre>
 */
public record ProfilingJsonProvider(JsonProvider delegate) implements JsonProvider {
    /**
     * Default constructor for service loading.
     *
     * <p>Loads and wraps another JsonProvider found via service loading.
     * If no provider or multiple providers are found, throws {@link IllegalStateException}.</p>
     *
     * @throws IllegalStateException if no JsonProvider is found on the classpath
     */
    public ProfilingJsonProvider() {
        this(loadDelegate());
    }

    /**
     * Creates a profiling wrapper around an existing JsonProvider.
     *
     * @param delegate the underlying provider to wrap and profile
     */
    public ProfilingJsonProvider {
    }

    private static JsonProvider loadDelegate() {
        ServiceLoader<JsonProvider> loader = ServiceLoader.load(JsonProvider.class);

        List<JsonProvider> providers = new ArrayList<>(16);
        for (JsonProvider provider : loader) {
            // Skip ourselves to avoid infinite recursion
            if (provider.getClass() != ProfilingJsonProvider.class) {
                providers.add(provider);
            }
        }

        if (providers.isEmpty()) {
            throw new IllegalStateException(
                "No JsonProvider found on classpath. " +
                    "Add a JSON library dependency (e.g., DslJsonProvider, JacksonJsonProvider). " +
                    "ProfilingJsonProvider requires an underlying JSON provider to wrap."
            );
        }

        if (providers.size() > 1) {
            List<String> list = new ArrayList<>(providers.size());
            for (JsonProvider p : providers) {
                list.add(p.getClass().getName());
            }
            throw new IllegalStateException(
                "More than one underlying JsonProvider found on classpath. " +
                    "Add only one JSON library dependency. Found: " + list
            );
        }

        JsonProvider first = providers.getFirst();
        if (first == null) {
            throw new IllegalStateException("Failed to load JsonProvider delegate");
        }
        return first;
    }

    @Override
    public String toJson(Object value) {
        if (!MagnesiumDebugger.isEnabled()) {
            return delegate.toJson(value);
        }

        long start = System.nanoTime();
        try {
            return delegate.toJson(value);
        } finally {
            long nanos = System.nanoTime() - start;
            Class<?> type = value != null ? value.getClass() : Object.class;
            MagnesiumDebugger.responseSerialized(type, nanos);
        }
    }

    @Override
    public byte[] toJsonBytes(Object value) {
        if (!MagnesiumDebugger.isEnabled()) {
            return delegate.toJsonBytes(value);
        }

        long start = System.nanoTime();
        try {
            return delegate.toJsonBytes(value);
        } finally {
            long nanos = System.nanoTime() - start;
            Class<?> type = value != null ? value.getClass() : Object.class;
            MagnesiumDebugger.responseSerialized(type, nanos);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        if (!MagnesiumDebugger.isEnabled()) {
            return delegate.fromJson(json, type);
        }

        long start = System.nanoTime();
        try {
            return delegate.fromJson(json, type);
        } finally {
            long nanos = System.nanoTime() - start;
            MagnesiumDebugger.requestDeserialized(type, nanos);
        }
    }

    @Override
    public <T> T fromRequest(Request request, Class<T> type) {
        if (!MagnesiumDebugger.isEnabled()) {
            return delegate.fromRequest(request, type);
        }

        long start = System.nanoTime();
        try {
            return delegate.fromRequest(request, type);
        } finally {
            long nanos = System.nanoTime() - start;
            MagnesiumDebugger.requestDeserialized(type, nanos);
        }
    }

    @Override
    public <T> ResponseEntity<byte[]> toResponse(T value) {
        if (!MagnesiumDebugger.isEnabled()) {
            return delegate.toResponse(value);
        }

        long start = System.nanoTime();
        try {
            return delegate.toResponse(value);
        } finally {
            long nanos = System.nanoTime() - start;
            Class<?> type = value != null ? value.getClass() : Object.class;
            MagnesiumDebugger.responseSerialized(type, nanos);
        }
    }

    /**
     * Writes JSON directly to an output stream with profiling.
     *
     * <p>This is an extension method that optimized JSON providers may implement
     * for zero-copy serialization. Falls back to {@link #toJsonBytes(Object)} if
     * the delegate doesn't support streaming.</p>
     *
     * @param value  the object to serialize
     * @param stream the output stream to write to
     * @throws JsonException if serialization fails
     */
    public void toStream(Object value, OutputStream stream) {
        if (!MagnesiumDebugger.isEnabled()) {
            // Fallback: serialize to bytes then write
            try {
                byte[] bytes = delegate.toJsonBytes(value);
                stream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write to stream", e);
            }
            return;
        }

        long start = System.nanoTime();
        try {
            byte[] bytes = delegate.toJsonBytes(value);
            stream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to stream", e);
        } finally {
            long nanos = System.nanoTime() - start;
            Class<?> type = value != null ? value.getClass() : Object.class;
            MagnesiumDebugger.responseSerialized(type, nanos);
        }
    }

    /**
     * Deserializes from a JSON type token with profiling.
     *
     * <p>This method supports generic type deserialization using a TypeToken pattern.</p>
     *
     * @param json the JSON string
     * @param type the generic type (e.g., {@code new TypeToken<List<User>>(){}.getType()})
     * @param <T>  the target type
     * @return deserialized object
     */
    public <T> T fromJson(String json, Type type) {
        if (!MagnesiumDebugger.isEnabled()) {
            // Try to delegate if supported, otherwise fall back
            return delegate.fromJson(json, (Class<T>) Object.class);
        }

        long start = System.nanoTime();
        try {
            return delegate.fromJson(json, (Class<T>) Object.class);
        } finally {
            long nanos = System.nanoTime() - start;
            Class<?> rawType = getRawType(type);
            MagnesiumDebugger.requestDeserialized(rawType, nanos);
        }
    }

    /**
     * Creates a pre-bound type-specific profiler for this provider.
     *
     * <p>Returns a provider that is bound to a specific type for more accurate
     * profiling of repeated operations on the same type.</p>
     *
     * @param type the type to bind to
     * @return a type-bound profiling provider
     */
    public JsonProvider withType(Type type) {
        return new TypeBoundProfilingProvider(delegate, type);
    }

    private Class<?> getRawType(Type type) {
        if (type instanceof Class<?> c) {
            return c;
        }
        // Extract raw class from generic type if possible
        String typeName = type.getTypeName();
        if (typeName.contains("<")) {
            typeName = typeName.substring(0, typeName.indexOf("<"));
        }
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            return Object.class;
        }
    }

    /**
     * A type-bound profiling provider that tracks operations for a specific type.
     */
    private static final class TypeBoundProfilingProvider implements JsonProvider {
        private final JsonProvider delegate;
        private final Class<?> boundType;

        TypeBoundProfilingProvider(JsonProvider delegate, Type type) {
            this.delegate = delegate;
            this.boundType = type instanceof Class<?> c ? c : Object.class;
        }

        @Override
        public String toJson(Object value) {
            long start = System.nanoTime();
            try {
                return delegate.toJson(value);
            } finally {
                long nanos = System.nanoTime() - start;
                MagnesiumDebugger.responseSerialized(boundType, nanos);
            }
        }

        @Override
        public byte[] toJsonBytes(Object value) {
            long start = System.nanoTime();
            try {
                return delegate.toJsonBytes(value);
            } finally {
                long nanos = System.nanoTime() - start;
                MagnesiumDebugger.responseSerialized(boundType, nanos);
            }
        }

        @Override
        public <T> T fromJson(String json, Class<T> type) {
            long start = System.nanoTime();
            try {
                return delegate.fromJson(json, type);
            } finally {
                long nanos = System.nanoTime() - start;
                MagnesiumDebugger.requestDeserialized(boundType, nanos);
            }
        }

        @Override
        public <T> T fromRequest(Request request, Class<T> type) {
            long start = System.nanoTime();
            try {
                return delegate.fromRequest(request, type);
            } finally {
                long nanos = System.nanoTime() - start;
                MagnesiumDebugger.requestDeserialized(boundType, nanos);
            }
        }

        @Override
        public <T> ResponseEntity<byte[]> toResponse(T value) {
            long start = System.nanoTime();
            try {
                return delegate.toResponse(value);
            } finally {
                long nanos = System.nanoTime() - start;
                MagnesiumDebugger.responseSerialized(boundType, nanos);
            }
        }
    }
}
