package net.magnesiumbackend.core.config.node;

import java.util.Map;

public record ConfigObject(Map<String, ConfigNode> values) implements ConfigNode {

    public ConfigNode get(String key) {
        return values.get(key);
    }
}