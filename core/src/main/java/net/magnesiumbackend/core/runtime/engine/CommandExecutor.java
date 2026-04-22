package net.magnesiumbackend.core.runtime.engine;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for command execution.
 *
 * <p>Implemented by the shell engine to execute commands from any input source.</p>
 */
@FunctionalInterface
public interface CommandExecutor {

    /**
     * Executes a command line.
     *
     * @param command the command line to execute
     * @return exit code (0 for success, non-zero for failure)
     */
    int execute(@NotNull String command);

    /**
     * Executes a command with arguments.
     *
     * @param command the command name
     * @param args the arguments
     * @return exit code
     */
    default int execute(@NotNull String command, @NotNull String... args) {
        StringBuilder sb = new StringBuilder(command);
        for (String arg : args) {
            sb.append(' ').append(arg);
        }
        return execute(sb.toString());
    }
}
