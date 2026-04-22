package net.magnesiumbackend.shell.input;

import net.magnesiumbackend.shell.completion.CompletionTree;
import net.magnesiumbackend.shell.engine.ShellEngine;
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
 * shell engine. Supports tab completion and history.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * ShellEngine engine = new ShellEngine(registry::resolve);
 * ShellInputSource inputSource = new ShellInputSource(commands, engine);
 * inputSource.start();
 * }</pre>
 */
public final class ShellInputSource {

    private static final Logger logger = Logger.getLogger(ShellInputSource.class.getName());

    private final Map<String, CommandIR> commands;
    private final CompletionTree completionTree;
    private final String prompt;
    private final ShellEngine engine;

    private volatile boolean running = false;
    private ExecutorService executorService;

    /**
     * Creates a shell input source.
     *
     * @param commands available commands for completion
     * @param engine the shell engine for executing commands
     */
    public ShellInputSource(@NotNull Map<String, CommandIR> commands, @NotNull ShellEngine engine) {
        this(commands, engine, "$ ");
    }

    /**
     * Creates a shell input source with custom prompt.
     *
     * @param commands available commands
     * @param engine the shell engine for executing commands
     * @param prompt the shell prompt
     */
    public ShellInputSource(@NotNull Map<String, CommandIR> commands, @NotNull ShellEngine engine, @NotNull String prompt) {
        this.commands = commands;
        this.engine = engine;
        this.completionTree = new CompletionTree();
        commands.values().forEach(completionTree::addCommand);
        this.prompt = prompt;
    }

    /**
     * Starts the shell input source.
     */
    public void start() {
        this.running = true;

        // Use virtual threads for input processing
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();

        executorService.submit(this::runInputLoop);

        logger.info("ShellInputSource started with " + commands.size() + " commands");
    }

    /**
     * Stops the shell input source.
     */
    public void stop() {
        running = false;

        if (executorService != null) {
            executorService.shutdownNow();
        }

        logger.info("ShellInputSource stopped");
    }

    private void runInputLoop() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        label:
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

            switch (line) {
                case "":
                    continue;


                    // Special commands
                case "help":
                    printHelp();
                    continue;
                case "exit":
                case "quit":
                    break label;
            }

            // Tab completion request (user typed command followed by ??)
            if (line.endsWith("??")) {
                String prefix = line.substring(0, line.length() - 2).trim();
                showCompletions(prefix);
                continue;
            }

            // Execute the command
            try {
                engine.execute(line);
            } catch (ShellEngine.CommandNotFoundException e) {
                System.err.println("Error: " + e.getMessage());
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

    /**
     * Returns the name of this input source.
     *
     * @return "shell-stdin"
     */
    public @NotNull String name() {
        return "shell-stdin";
    }
}
