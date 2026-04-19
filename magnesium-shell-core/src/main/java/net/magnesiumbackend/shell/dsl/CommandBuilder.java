package net.magnesiumbackend.shell.dsl;

import net.magnesiumbackend.shell.ir.ArgumentSchema;
import net.magnesiumbackend.shell.ir.CommandIR;
import net.magnesiumbackend.shell.ir.ExecutionMode;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Fluent DSL for defining commands programmatically.
 *
 * <p>Equivalent expressiveness to annotation API:</p>
 * <pre>{@code
 * CommandBuilder.create("user:create")
 *     .description("Create a new user")
 *     .arg("email", String.class)
 *         .required()
 *         .description("User email")
 *         .add()
 *     .arg("role", String.class)
 *         .defaultValue("USER")
 *         .add()
 *     .action(ctx -> {
 *         String email = ctx.arg("email");
 *         String role = ctx.arg("role");
 *         // ...
 *     });
 * }</pre>
 *
 * @see CommandContext
 */
public final class CommandBuilder {

    private final CommandIR.Builder irBuilder;
    private final ArgumentSchema.Builder schemaBuilder;
    private Consumer<CommandContext> action;

    private CommandBuilder(String name) {
        this.irBuilder = CommandIR.builder(name);
        this.schemaBuilder = ArgumentSchema.builder();
    }

    /**
     * Creates a new command builder.
     *
     * @param name the command name
     * @return a new builder
     */
    public static CommandBuilder create(@NotNull String name) {
        return new CommandBuilder(name);
    }

    public CommandBuilder description(@NotNull String description) {
        irBuilder.description(description);
        return this;
    }

    public CommandBuilder mode(@NotNull ExecutionMode mode) {
        irBuilder.executionMode(mode);
        return this;
    }

    public CommandBuilder amqpBinding(@NotNull String binding) {
        irBuilder.amqpBinding(binding);
        return this;
    }

    public CommandBuilder alias(@NotNull String alias) {
        irBuilder.alias(alias);
        return this;
    }

    public ArgBuilder arg(@NotNull String name, @NotNull Class<?> type) {
        return new ArgBuilder(this, name, type);
    }

    CommandBuilder addArgument(ArgumentSchema.ArgumentDef def) {
        schemaBuilder.addArgument(def);
        return this;
    }

    public CommandBuilder action(@NotNull Consumer<CommandContext> action) {
        this.action = action;
        return this;
    }

    public CommandIR buildIR(@NotNull String handlerClass, @NotNull String handlerMethod) {
        return irBuilder
            .arguments(schemaBuilder.build())
            .handlerClass(handlerClass)
            .handlerMethod(handlerMethod)
            .build();
    }

    public Consumer<CommandContext> action() {
        return action;
    }

    /**
     * Builder for command arguments.
     */
    public static final class ArgBuilder {
        private final CommandBuilder parent;
        private final String name;
        private Class<?> type;
        private boolean required = false;
        private Object defaultValue = null;
        private String description = "";
        private boolean positional = false;
        private boolean flag = false;

        ArgBuilder(CommandBuilder parent, String name, Class<?> type) {
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

        public CommandBuilder add() {
            ArgumentSchema.ArgumentDef def = new ArgumentSchema.ArgumentDef(
                name, type, required, defaultValue,
                description, positional, flag
            );
            return parent.addArgument(def);
        }
    }
}
