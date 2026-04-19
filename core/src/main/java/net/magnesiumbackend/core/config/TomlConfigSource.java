package net.magnesiumbackend.core.config;

import net.magnesiumbackend.core.config.node.ConfigArray;
import net.magnesiumbackend.core.config.node.ConfigNode;
import net.magnesiumbackend.core.config.node.ConfigObject;
import net.magnesiumbackend.core.config.node.ConfigValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * TOML configuration source with:
 * - typed AST (ConfigNode)
 * - precomputed dotted-key index for O(1) lookup
 */
public class TomlConfigSource implements ConfigSource {

    private final String name;
    private final ConfigNode root;
    private final Map<String, ConfigNode> index;

    private TomlConfigSource(
        @NotNull String name,
        @NotNull ConfigNode root,
        @NotNull Map<String, ConfigNode> index
    ) {
        this.name = name;
        this.root = root;
        this.index = index;
    }

    public static @NotNull TomlConfigSource fromPath(@NotNull Path path) {
        try {
            SeekableByteChannel content = Files.newByteChannel(path);
            return fromString("toml:" + path, content);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read TOML: " + path, e);
        }
    }

    public static @NotNull TomlConfigSource fromString(@NotNull String name, @NotNull SeekableByteChannel content) throws IOException {
        TomlParseResult result = Toml.parse(content);

        if (result.hasErrors()) {
            throw new IllegalStateException(
                "Invalid TOML: " + name + "\n" + result.errors()
            );
        }

        Map<String, Object> map = result.toMap();
        ConfigNode root = convert(map);
        Map<String, ConfigNode> index = new HashMap<>(map.size());

        buildIndex("", root, index);

        return new TomlConfigSource(name, root, index);
    }

    @Override
    public boolean has(@NotNull String key) {
        return index.containsKey(key);
    }

    @Override
    public @Nullable String getString(@NotNull String key) {
        ConfigNode node = index.get(key);
        return node instanceof ConfigValue v ? v.asString() : null;
    }

    @Override
    public @Nullable Integer getInt(@NotNull String key) {
        ConfigNode node = index.get(key);
        return node instanceof ConfigValue v ? v.asInt() : null;
    }

    @Override
    public @Nullable Long getLong(@NotNull String key) {
        ConfigNode node = index.get(key);
        return node instanceof ConfigValue v ? v.asLong() : null;
    }

    @Override
    public @Nullable Boolean getBoolean(@NotNull String key) {
        ConfigNode node = index.get(key);
        return node instanceof ConfigValue v ? v.asBoolean() : null;
    }

    @Override
    public @Nullable Double getDouble(@NotNull String key) {
        ConfigNode node = index.get(key);
        return node instanceof ConfigValue v ? v.asDouble() : null;
    }

    @Override
    public @Nullable Float getFloat(@NotNull String key) {
        ConfigNode node = index.get(key);
        return node instanceof ConfigValue v ? v.asDouble().floatValue() : null;
    }

    @Override
    public <E extends Enum<E>> @Nullable E getEnum(@NotNull String key, @NotNull Class<E> enumType) {
        String raw = getString(key);
        if (raw == null) return null;

        try {
            return Enum.valueOf(enumType, raw.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    private static void buildIndex(
        String prefix,
        ConfigNode node,
        Map<String, ConfigNode> index
    ) {
        if (node instanceof ConfigObject obj) {
            for (var entry : obj.values().entrySet()) {
                String key = prefix.isEmpty()
                    ? entry.getKey()
                    : prefix + "." + entry.getKey();

                buildIndex(key, entry.getValue(), index);
            }
        } else {
            index.put(prefix, node);
        }
    }

    private static ConfigNode convert(Object value) {

        if (value instanceof Map<?, ?> map) {
            Map<String, ConfigNode> result = new HashMap<>(map.size());

            for (var e : map.entrySet()) {
                result.put(
                    e.getKey().toString(),
                    convert(e.getValue())
                );
            }

            return new ConfigObject(result);
        }

        if (value instanceof java.util.List<?> list) {
            java.util.List<ConfigNode> out = new java.util.ArrayList<>(list.size());
            for (Object v : list) {
                out.add(convert(v));
            }
            return new ConfigArray(out);
        }

        return new ConfigValue(value);
    }
}