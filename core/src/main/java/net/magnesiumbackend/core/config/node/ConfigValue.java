package net.magnesiumbackend.core.config.node;

public final class ConfigValue implements ConfigNode {
    private final Object value;

    public ConfigValue(Object value) {
        this.value = value;
    }

    public Object raw() {
        return value;
    }

    public String asString() {
        return value.toString();
    }

    public Integer asInt() {
        return value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
    }

    public Boolean asBoolean() {
        return value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
    }

    public Long asLong() {
        return value instanceof Number n ? n.longValue() : Long.parseLong(value.toString());
    }

    public Double asDouble() {
        return value instanceof Number n ? n.doubleValue() : Double.parseDouble(value.toString());
    }
}