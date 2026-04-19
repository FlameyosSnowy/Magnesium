package net.magnesiumbackend.shell.ir;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Intermediate Representation of a CLI command.
 *
 * <p>This is the canonical data structure that all commands compile into.
 * Both annotation-defined and DSL-defined commands reduce to this IR.</p>
 *
 * <pre>{@code
 * CommandIR {
 *     name: "user:create"
 *     args: TypedSchema { email: String, role: String }
 *     handler: GeneratedHandler#handle
 *     executionMode: LOCAL | DATA | AMQP
 * }
 * }</pre>
 *
 * @see ExecutionMode
 * @see ArgumentSchema
 */
public final class CommandIR {

    private final String name;
    private final String description;
    private final ArgumentSchema arguments;
    private final String handlerClass;
    private final String handlerMethod;
    private final ExecutionMode executionMode;
    private final String amqpBinding;
    private final List<String> aliases;

    private CommandIR(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name");
        this.description = builder.description;
        this.arguments = Objects.requireNonNull(builder.arguments, "arguments");
        this.handlerClass = Objects.requireNonNull(builder.handlerClass, "handlerClass");
        this.handlerMethod = Objects.requireNonNull(builder.handlerMethod, "handlerMethod");
        this.executionMode = Objects.requireNonNull(builder.executionMode, "executionMode");
        this.amqpBinding = builder.amqpBinding;
        this.aliases = List.copyOf(builder.aliases);
    }

    /**
     * Creates a new builder.
     *
     * @param name the command name
     * @return a new builder
     */
    public static Builder builder(@NotNull String name) {
        return new Builder(name);
    }

    public @NotNull String name() {
        return name;
    }

    public @Nullable String description() {
        return description;
    }

    public @NotNull ArgumentSchema arguments() {
        return arguments;
    }

    public @NotNull String handlerClass() {
        return handlerClass;
    }

    public @NotNull String handlerMethod() {
        return handlerMethod;
    }

    public @NotNull ExecutionMode executionMode() {
        return executionMode;
    }

    public @Nullable String amqpBinding() {
        return amqpBinding;
    }

    public @NotNull List<String> aliases() {
        return aliases;
    }

    /**
     * Builder for CommandIR.
     */
    public static final class Builder {
        private final String name;
        private String description;
        private ArgumentSchema arguments = ArgumentSchema.empty();
        private String handlerClass;
        private String handlerMethod = "execute";
        private ExecutionMode executionMode = ExecutionMode.LOCAL;
        private String amqpBinding;
        private List<String> aliases = new ArrayList<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder arguments(ArgumentSchema arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder handlerClass(String handlerClass) {
            this.handlerClass = handlerClass;
            return this;
        }

        public Builder handlerMethod(String handlerMethod) {
            this.handlerMethod = handlerMethod;
            return this;
        }

        public Builder executionMode(ExecutionMode mode) {
            this.executionMode = mode;
            return this;
        }

        public Builder amqpBinding(String binding) {
            this.amqpBinding = binding;
            return this;
        }

        public Builder alias(String alias) {
            this.aliases.add(alias);
            return this;
        }

        public CommandIR build() {
            return new CommandIR(this);
        }
    }

    @Override
    public String toString() {
        return "CommandIR{name='" + name + "', mode=" + executionMode + "}";
    }
}
