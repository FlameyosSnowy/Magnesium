package net.magnesiumbackend.shell.ir;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Typed schema for command arguments.
 *
 * <p>Defines the structure and types of all arguments a command accepts.
 * Used for validation, parsing, and help generation.</p>
 *
 * <pre>{@code
 * ArgumentSchema schema = ArgumentSchema.builder()
 *     .arg("email", String.class).required()
 *     .arg("role", String.class).defaultValue("USER")
 *     .arg("active", boolean.class).flag()
 *     .build();
 * }</pre>
 */
public final class ArgumentSchema {

    private final Map<String, ArgumentDef> arguments;
    private final List<String> positionalOrder;

    private ArgumentSchema(Map<String, ArgumentDef> arguments, List<String> positionalOrder) {
        this.arguments = Map.copyOf(arguments);
        this.positionalOrder = List.copyOf(positionalOrder);
    }

    /**
     * Returns an empty schema.
     *
     * @return empty schema
     */
    public static ArgumentSchema empty() {
        return new ArgumentSchema(Map.of(), List.of());
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets an argument definition by name.
     *
     * @param name argument name
     * @return argument definition or null
     */
    public @Nullable ArgumentDef get(@NotNull String name) {
        return arguments.get(name);
    }

    public boolean has(@NotNull String name) {
        return arguments.containsKey(name);
    }

    /**
     * Returns all argument definitions.
     *
     * @return unmodifiable map of arguments
     */
    public @NotNull Map<String, ArgumentDef> all() {
        return arguments;
    }

    /**
     * Returns the positional argument order.
     *
     * @return ordered list of argument names
     */
    public @NotNull List<String> positionalOrder() {
        return positionalOrder;
    }

    /**
     * Validates that the given values satisfy this schema.
     *
     * @param values the values to validate
     * @throws CommandValidationException if validation fails
     */
    public void validate(@NotNull Map<String, Object> values) {
        for (ArgumentDef arg : arguments.values()) {
            Object value = values.get(arg.name());

            if (value == null) {
                if (arg.required() && arg.defaultValue() == null) {
                    throw new CommandValidationException(
                        "Missing required argument: " + arg.name());
                }
                continue;
            }

            if (!arg.type().isInstance(value)) {
                throw new CommandValidationException(
                    "Argument '" + arg.name() + "' expected " + arg.type().getSimpleName() +
                    " but got " + value.getClass().getSimpleName());
            }
        }
    }

    @Override
    public String toString() {
        return "ArgumentSchema{args=" + arguments.keySet() + "}";
    }

    /**
     * Builder for ArgumentSchema.
     */
    public static final class Builder {
        private final Map<String, ArgumentDef> arguments = new LinkedHashMap<>();
        private final List<String> positionalOrder = new ArrayList<>();

        public ArgBuilder arg(@NotNull String name, @NotNull Class<?> type) {
            return new ArgBuilder(this, name, type);
        }

        public Builder addArgument(ArgumentDef def) {
            arguments.put(def.name(), def);
            if (def.positional()) {
                positionalOrder.add(def.name());
            }
            return this;
        }

        public ArgumentSchema build() {
            return new ArgumentSchema(arguments, positionalOrder);
        }
    }

    /**
     * Builder for a single argument.
     */
    public static final class ArgBuilder {
        private final Builder parent;
        private final String name;
        private Class<?> type;
        private boolean required = false;
        private Object defaultValue = null;
        private String description = "";
        private boolean positional = false;
        private boolean flag = false;

        ArgBuilder(Builder parent, String name, Class<?> type) {
            this.parent = parent;
            this.name = name;
            this.type = type;
        }

        public ArgBuilder required() {
            this.required = true;
            return this;
        }

        public ArgBuilder defaultValue(Object value) {
            this.defaultValue = value;
            return this;
        }

        public ArgBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ArgBuilder positional() {
            this.positional = true;
            return this;
        }

        public ArgBuilder flag() {
            this.flag = true;
            this.type = boolean.class;
            return this;
        }

        public Builder add() {
            ArgumentDef def = new ArgumentDef(
                name, type, required, defaultValue,
                description, positional, flag
            );
            return parent.addArgument(def);
        }
    }

    /**
     * Definition of a single argument.
     */
    public record ArgumentDef(
        @NotNull String name,
        @NotNull Class<?> type,
        boolean required,
        @Nullable Object defaultValue,
        @NotNull String description,
        boolean positional,
        boolean flag
    ) {}

    /**
     * Exception thrown during command validation.
     */
    public static class CommandValidationException extends RuntimeException {
        public CommandValidationException(String message) {
            super(message);
        }
    }
}
