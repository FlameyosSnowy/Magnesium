package net.magnesiumbackend.shell.dsl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Runtime context for command execution.
 *
 * <p>Provides access to parsed arguments and execution metadata.</p>
 *
 * <pre>{@code
 * .action(ctx -> {
 *     String email = ctx.arg("email");
 *     boolean verbose = ctx.flag("verbose");
 *     // ...
 * })
 * }</pre>
 */
public final class CommandContext {

    private final String commandName;
    private final Map<String, Object> arguments;

    public CommandContext(@NotNull String commandName, @NotNull Map<String, Object> arguments) {
        this.commandName = Objects.requireNonNull(commandName, "commandName");
        this.arguments = Map.copyOf(arguments);
    }

    /**
     * Gets the command name.
     *
     * @return command name
     */
    public @NotNull String commandName() {
        return commandName;
    }

    /**
     * Gets an argument value.
     *
     * @param name argument name
     * @return argument value or null
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T arg(@NotNull String name) {
        return (T) arguments.get(name);
    }

    /**
     * Gets an argument value with default.
     *
     * @param name argument name
     * @param defaultValue default if not present
     * @return argument value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T argOrDefault(@NotNull String name, T defaultValue) {
        Object value = arguments.get(name);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Checks if a boolean flag is set.
     *
     * @param name flag name
     * @return true if flag is present and true
     */
    public boolean flag(@NotNull String name) {
        Object value = arguments.get(name);
        return Boolean.TRUE.equals(value);
    }

    /**
     * Returns all arguments.
     *
     * @return unmodifiable map of arguments
     */
    public @NotNull Map<String, Object> allArgs() {
        return arguments;
    }

    @Override
    public String toString() {
        return "CommandContext{command='" + commandName + "', args=" + arguments.keySet() + "}";
    }
}
