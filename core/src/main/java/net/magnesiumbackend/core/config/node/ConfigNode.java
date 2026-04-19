package net.magnesiumbackend.core.config.node;

public sealed interface ConfigNode permits ConfigObject, ConfigArray, ConfigValue {}