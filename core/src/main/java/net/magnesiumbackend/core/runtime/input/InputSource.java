package net.magnesiumbackend.core.runtime.input;

import net.magnesiumbackend.core.runtime.engine.CommandExecutor;
import org.jetbrains.annotations.NotNull;

/**
 * Input source for commands.
 *
 * <p>Implementations provide commands from various sources:</p>
 * <ul>
 *   <li>{@link ShellInputSource} - Interactive shell / stdin</li>
 *   <li>{@link AmqpInputSource} - AMQP message queue</li>
 *   <li>{@link HttpInputSource} - HTTP requests</li>
 *   <li>{@link ScheduledInputSource} - Cron-like scheduling</li>
 * </ul>
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>Kernel calls {@link #start(CommandExecutor)}</li>
 *   <li>Input source reads input and submits commands to executor</li>
 *   <li>Kernel calls {@link #stop()} on shutdown</li>
 * </ol>
 */
public interface InputSource {

    /**
     * Starts the input source.
     *
     * <p>Called by the runtime kernel during startup. The input source
     * should begin listening for input and executing commands via the
     * provided executor.</p>
     *
     * <p>This method should return quickly (non-blocking). The actual
     * input processing should happen on a separate thread.</p>
     *
     * @param executor the command executor
     */
    void start(@NotNull CommandExecutor executor);

    /**
     * Stops the input source.
     *
     * <p>Called by the runtime kernel during shutdown. The input source
     * should stop accepting new input and complete any pending commands.</p>
     */
    void stop();

    /**
     * Returns the name of this input source.
     *
     * @return input source name
     */
    default @NotNull String name() {
        return getClass().getSimpleName();
    }
}
