package net.magnesiumbackend.core.config.node;

import java.util.List;

public record ConfigArray(List<ConfigNode> values) implements ConfigNode {
}