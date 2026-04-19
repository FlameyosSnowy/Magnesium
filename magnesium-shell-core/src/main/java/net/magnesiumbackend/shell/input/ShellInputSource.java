package net.magnesiumbackend.shell.input;

import net.magnesiumbackend.core.runtime.engine.CommandExecutor;
import net.magnesiumbackend.core.runtime.input.InputSource;
import net.magnesiumbackend.shell.completion.CompletionTree;
import net.magnesiumbackend.shell.ir.CommandIR;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Input source for interactive shell / stdin.
 *
 * <p>Reads commands from standard input and executes them via the
 * command executor. Supports tab completion and history.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * RuntimeConfig config = RuntimeConfig.builder()
 *     .inputSource(new ShellInputSource(commands))
 *     .lifecyclePolicy(new ShellLifecyclePolicy())
 *     .build();
 * }</pre>
 */
public final class ShellInputSource implements InputSource {

    private static final Logger logger = Logger.getLogger(ShellInputSource.class.getName());

    private final Map<String, CommandIR> commands;
    private final CompletionTree completionTree;
    private final String prompt;

    private volatile boolean running = false;
    private volatile CommandExecutor executor;
    private ExecutorService executorService;

    /**
     * Creates a shell input source.
     *
     * @param commands available commands for completion
     */
    public ShellInputSource(@NotNull Map<String, CommandIR> commands) {
        this(commands, "$ ");
    }

    /**
     * Creates a shell input source with custom prompt.
     *
     * @param commands available commands
     * @param prompt the shell prompt
     */
    public ShellInputSource(@NotNull Map<String, CommandIR> commands, @NotNull String prompt) {
        this.commands = commands;
        this.completionTree = new CompletionTree();
        commands.values().forEach(completionTree::addCommand);
        this.prompt = prompt;
    }

    @Override
    public void start(@NotNull CommandExecutor executor) {
        this.executor = executor;
        this.running = true;

        // Use virtual threads for input processing
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();

        executorService.submit(this::runInputLoop);

        logger.info("ShellInputSource started with " + commands.size() + " commands");
    }

    @Override
    public void stop() {
        running = false;

        if (executorService != null) {
            executorService.shutdownNow();
        }

        logger.info("ShellInputSource stopped");
    }

    private void runInputLoop() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (running) {
            System.out.print(prompt);
            System.out.flush();

            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                logger.warning("Error reading from stdin: " + e.getMessage());
                break;
            }

            if (line == null) {
                // EOF reached
                System.out.println();
                break;
            }

            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            // Special commands
            if (line.equals("help")) {
                printHelp();
                continue;
            }

            if (line.equals("exit") || line.equals("quit")) {
                break;
            }

            // Tab completion request (user typed command followed by ??)
            if (line.endsWith("??")) {
                String prefix = line.substring(0, line.length() - 2).trim();
                showCompletions(prefix);
                continue;
            }

            // Execute the command
            try {
                int exitCode = executor.execute(line);
                if (exitCode != 0) {
                    System.err.println("Command failed with exit code: " + exitCode);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private void showCompletions(String prefix) {
        var suggestions = completionTree.complete(prefix, prefix.length());

        if (suggestions.isEmpty()) {
            System.out.println("  (no matches)");
        } else {
            System.out.println("  " + String.join("  ", suggestions));
        }
    }

    private void printHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("─────────────────────────────────────────────────────────");

        for (CommandIR cmd : commands.values()) {
            String desc = cmd.description() != null ? cmd.description() : "";
            if (desc.length() > 40) {
                desc = desc.substring(0, 37) + "...";
            }
            System.out.printf("  %-30s %s%n", cmd.name(), desc);
        }

        System.out.println("\nSpecial Commands:");
        System.out.println("  help              Show this help");
        System.out.println("  exit/quit         Exit the shell");
        System.out.println("  <command>??       Show command completions");
        System.out.println("─────────────────────────────────────────────────────────\n");
    }

    @Override
    public @NotNull String name() {
        return "shell-stdin";
    }
}
