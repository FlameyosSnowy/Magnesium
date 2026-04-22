package net.magnesiumbackend.core.runtime.lifecycle;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * Lifecycle policy for interactive shell.
 *
 * <p>The shell owns the main thread, blocking on stdin. Shutdown occurs
 * when the user types 'exit' or sends EOF (Ctrl+D).</p>
 *
 * <p>Best for CLI applications where the shell is the primary interface.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * RuntimeConfig config = RuntimeConfig.builder()
 *     .inputSource(new ShellInputSource()) // Shell reads commands
 *     .lifecyclePolicy(new ShellLifecyclePolicy()) // Main thread is shell
 *     .build();
 * }</pre>
 */
public final class ShellLifecyclePolicy implements LifecyclePolicy {

    private static final Logger logger = Logger.getLogger(ShellLifecyclePolicy.class.getName());

    private final String prompt;

    /**
     * Creates a shell lifecycle policy with the given prompt.
     *
     * @param prompt the shell prompt string
     */
    public ShellLifecyclePolicy(String prompt) {
        this.prompt = prompt;
    }

    /**
     * Creates a shell lifecycle policy with default "$ " prompt.
     */
    public ShellLifecyclePolicy() {
        this("$ ");
    }

    @Override
    public void blockUntilShutdown(@NotNull ShutdownContext context) {
        logger.info("Shell lifecycle started. Type 'exit' or press Ctrl+D to quit.");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (!context.isShutdownRequested()) {
            System.out.print(prompt);
            System.out.flush();

            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                logger.warning("Error reading from stdin: " + e.getMessage());
                context.requestShutdown();
                break;
            }

            // EOF reached (Ctrl+D)
            if (line == null) {
                System.out.println();
                logger.info("EOF received, shutting down");
                context.requestShutdown();
                break;
            }

            line = line.trim();

            // Empty line - just show prompt again
            if (line.isEmpty()) {
                continue;
            }

            // Exit command
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                logger.info("Exit command received");
                context.requestShutdown();
                break;
            }

            // Shutdown is handled by the shell input source
            // This policy just monitors for explicit exit commands
        }
    }
}
