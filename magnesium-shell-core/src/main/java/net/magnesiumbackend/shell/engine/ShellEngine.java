package net.magnesiumbackend.shell.engine;

import net.magnesiumbackend.shell.dsl.CommandContext;
import net.magnesiumbackend.shell.ir.ArgumentSchema;
import net.magnesiumbackend.shell.ir.CommandIR;
import net.magnesiumbackend.shell.ir.ExecutionMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Minimal runtime shell engine.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Tokenize CLI input</li>
 *   <li>Resolve command from generated registry</li>
 *   <li>Bind args to typed structures</li>
 *   <li>Invoke handler</li>
 * </ul>
 *
 * <p>Zero reflection, zero dynamic lookup.</p>
 *
 * <pre>{@code
 * ShellEngine engine = new ShellEngine(GeneratedCommandRegistry::getHandler);
 * engine.execute("user:create --email=foo@bar.com --role=ADMIN");
 * }</pre>
 */
public final class ShellEngine {

    private final CommandResolver resolver;
    private final ExecutionDelegate dataDelegate;
    private final ExecutionDelegate amqpDelegate;

    /**
     * Functional interface for command resolution.
     */
    @FunctionalInterface
    public interface CommandResolver {
        @Nullable GeneratedHandler resolve(@NotNull String name);
    }

    /**
     * Functional interface for execution delegation.
     */
    @FunctionalInterface
    public interface ExecutionDelegate {
        void execute(@NotNull CommandIR ir, @NotNull CommandContext ctx);
    }

    /**
     * Creates a shell engine with the given resolver.
     *
     * @param resolver function to resolve command names to handlers
     */
    public ShellEngine(@NotNull CommandResolver resolver) {
        this(resolver, null, null);
    }

    /**
     * Creates a shell engine with full delegation support.
     *
     * @param resolver resolves commands to handlers
     * @param dataDelegate handles DATA mode execution
     * @param amqpDelegate handles AMQP mode execution
     */
    public ShellEngine(
        @NotNull CommandResolver resolver,
        @Nullable ExecutionDelegate dataDelegate,
        @Nullable ExecutionDelegate amqpDelegate
    ) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.dataDelegate = dataDelegate;
        this.amqpDelegate = amqpDelegate;
    }

    /**
     * Executes a command line.
     *
     * @param line the command line input
     * * @throws CommandNotFoundException if command not found
     * @throws CommandValidationException if arguments invalid
     */
    public void execute(@NotNull String line) {
        List<String> tokens = tokenize(line);
        if (tokens.isEmpty()) return;

        String commandName = tokens.get(0);
        GeneratedHandler handler = resolver.resolve(commandName);

        if (handler == null) {
            throw new CommandNotFoundException("Command not found: " + commandName);
        }

        // For annotation-generated handlers, we need the IR
        // In practice, the handler knows its own IR or we look it up
        Map<String, Object> args = parseArguments(tokens.subList(1, tokens.size()));
        CommandContext ctx = new CommandContext(commandName, args);

        handler.execute(ctx);
    }

    /**
     * Executes a command with explicit IR (for DSL-defined commands).
     *
     * @param ir the command IR
     * @param handler the handler to execute
     * @param args raw argument tokens
     */
    public void execute(
        @NotNull CommandIR ir,
        @NotNull GeneratedHandler handler,
        @NotNull List<String> args
    ) {
        Map<String, Object> parsed = parseArgumentsWithSchema(ir.arguments(), args);
        CommandContext ctx = new CommandContext(ir.name(), parsed);

        // Validate
        ir.arguments().validate(parsed);

        // Execute based on mode
        switch (ir.executionMode()) {
            case LOCAL -> handler.execute(ctx);
            case DATA -> {
                if (dataDelegate == null) {
                    throw new IllegalStateException("No DATA delegate configured");
                }
                dataDelegate.execute(ir, ctx);
            }
            case AMQP -> {
                if (amqpDelegate == null) {
                    throw new IllegalStateException("No AMQP delegate configured");
                }
                amqpDelegate.execute(ir, ctx);
            }
        }
    }

    /**
     * Tokenizes a command line.
     *
     * @param line the input line
     * @return list of tokens
     */
    public @NotNull List<String> tokenize(@NotNull String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' || c == '\'') {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    private Map<String, Object> parseArguments(List<String> tokens) {
        Map<String, Object> args = new HashMap<>();
        int positionalIndex = 0;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if (token.startsWith("--")) {
                // Named argument
                String name = token.substring(2);
                Object value = Boolean.TRUE;

                int eqIndex = name.indexOf('=');
                if (eqIndex > 0) {
                    String val = name.substring(eqIndex + 1);
                    name = name.substring(0, eqIndex);
                    value = val;
                } else if (i + 1 < tokens.size() && !tokens.get(i + 1).startsWith("-")) {
                    value = tokens.get(++i);
                }

                args.put(name, value);
            } else if (token.startsWith("-")) {
                // Short flag(s)
                for (char c : token.substring(1).toCharArray()) {
                    args.put(String.valueOf(c), Boolean.TRUE);
                }
            } else {
                // Positional argument
                args.put("$" + positionalIndex++, token);
            }
        }

        return args;
    }

    private Map<String, Object> parseArgumentsWithSchema(
        ArgumentSchema schema,
        List<String> tokens
    ) {
        Map<String, Object> args = new HashMap<>();
        List<String> positionalArgs = new ArrayList<>();

        // First pass: collect named args and positional args
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if (token.startsWith("--")) {
                String name = token.substring(2);
                Object value = Boolean.TRUE;

                int eqIndex = name.indexOf('=');
                if (eqIndex > 0) {
                    value = name.substring(eqIndex + 1);
                    name = name.substring(0, eqIndex);
                } else if (i + 1 < tokens.size() && !tokens.get(i + 1).startsWith("-")) {
                    value = tokens.get(++i);
                }

                ArgumentSchema.ArgumentDef def = schema.get(name);
                if (def != null) {
                    args.put(name, convertType(value, def.type()));
                } else {
                    args.put(name, value);
                }
            } else if (token.startsWith("-")) {
                for (char c : token.substring(1).toCharArray()) {
                    args.put(String.valueOf(c), Boolean.TRUE);
                }
            } else {
                positionalArgs.add(token);
            }
        }

        // Map positional args to schema
        List<String> positionalOrder = schema.positionalOrder();
        for (int i = 0; i < positionalArgs.size() && i < positionalOrder.size(); i++) {
            String name = positionalOrder.get(i);
            ArgumentSchema.ArgumentDef def = schema.get(name);
            Object value = convertType(positionalArgs.get(i),
                def != null ? def.type() : String.class);
            args.put(name, value);
        }

        // Apply defaults
        for (ArgumentSchema.ArgumentDef def : schema.all().values()) {
            if (!args.containsKey(def.name()) && def.defaultValue() != null) {
                args.put(def.name(), def.defaultValue());
            }
        }

        return args;
    }

    @SuppressWarnings("unchecked")
    private Object convertType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        String s = value.toString();

        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(s);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(s);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(s);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(s);
        }

        return s;
    }

    /**
     * Exception thrown when a command is not found.
     */
    public static class CommandNotFoundException extends RuntimeException {
        public CommandNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when argument validation fails.
     */
    public static class CommandValidationException extends RuntimeException {
        public CommandValidationException(String message) {
            super(message);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INTERACTIVE MODE & COMPLETION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Starts an interactive REPL reading from stdin.
     *
     * <p>Provides tab completion, history, and help commands.</p>
     *
     * <pre>{@code
     * ShellEngine engine = new ShellEngine(registry::get);
     * engine.startInteractive(registry.getAllCommands(), registry::complete);
     * }</pre>
     *
     * @param commands map of available commands
     * @param completer function to provide tab completion suggestions
     */
    public void startInteractive(
        @NotNull Map<String, CommandIR> commands,
        @NotNull CompletionDelegate completer
    ) {
        System.out.println("Magnesium Shell Interactive Mode");
        System.out.println("Type 'help' for commands, 'exit' to quit.\n");

        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(System.in));
        java.util.List<String> history = new java.util.ArrayList<>();

        while (true) {
            System.out.print("$ ");
            System.out.flush();

            String line;
            try {
                // Check if stdin has data (non-blocking check)
                if (!reader.ready() && System.in.available() == 0) {
                    line = reader.readLine();
                } else {
                    line = reader.readLine();
                }
            } catch (java.io.IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
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

            history.add(line);

            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                System.out.println("Goodbye!");
                break;
            }

            if (line.equals("help")) {
                printHelp(commands);
                continue;
            }

            // Handle tab completion request (ends with TAB character or ??)
            if (line.endsWith("\t") || line.endsWith("??")) {
                String prefix = line.endsWith("??") ? line.substring(0, line.length() - 2) : line.substring(0, line.length() - 1);
                java.util.List<String> suggestions = completer.complete(prefix, prefix.length());
                if (suggestions.isEmpty()) {
                    System.out.println("  (no matches)");
                } else {
                    System.out.println("  " + String.join("  ", suggestions));
                }
                continue;
            }

            // Execute command
            try {
                execute(line);
            } catch (CommandNotFoundException e) {
                System.err.println("Error: " + e.getMessage());
                // Suggest similar commands
                java.util.List<String> suggestions = completer.complete(line, line.indexOf(' ') > 0 ? line.indexOf(' ') : line.length());
                if (!suggestions.isEmpty()) {
                    System.out.println("  Did you mean: " + String.join(", ", suggestions) + "?");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }

            System.out.println();
        }
    }

    /**
     * Starts interactive mode with a simplified completer.
     *
     * @param commands map of available commands
     */
    public void startInteractive(@NotNull Map<String, CommandIR> commands) {
        startInteractive(commands, (prefix, cursor) -> {
            String[] parts = prefix.substring(0, cursor).split("\\s+");
            String lastToken = parts.length > 0 ? parts[parts.length - 1] : "";

            java.util.List<String> matches = new java.util.ArrayList<>();
            for (String cmd : commands.keySet()) {
                if (cmd.startsWith(lastToken)) {
                    matches.add(cmd);
                }
            }
            return matches;
        });
    }

    private void printHelp(Map<String, CommandIR> commands) {
        System.out.println("\nAvailable Commands:");
        System.out.println("─────────────────────────────────────────────────────────");

        java.util.Map<String, java.util.List<String>> byNamespace = new java.util.TreeMap<>();
        for (String cmd : commands.keySet()) {
            String ns = cmd.contains(":") ? cmd.substring(0, cmd.indexOf(':')) : "general";
            byNamespace.computeIfAbsent(ns, k -> new java.util.ArrayList<>()).add(cmd);
        }

        for (java.util.Map.Entry<String, java.util.List<String>> entry : byNamespace.entrySet()) {
            System.out.println(entry.getKey() + ":");
            for (String cmd : entry.getValue()) {
                CommandIR ir = commands.get(cmd);
                String desc = ir != null && ir.description() != null ? ir.description() : "";
                if (desc.length() > 40) {
                    desc = desc.substring(0, 37) + "...";
                }
                System.out.printf("  %-30s %s%n", cmd, desc);
            }
        }

        System.out.println("\nSpecial Commands:");
        System.out.println("  help              Show this help");
        System.out.println("  exit/quit         Exit the shell");
        System.out.println("  <command>??       Show completions for command");
        System.out.println("─────────────────────────────────────────────────────────\n");
    }

    /**
     * Functional interface for tab completion.
     */
    @FunctionalInterface
    public interface CompletionDelegate {
        java.util.List<String> complete(@NotNull String prefix, int cursor);
    }
}
