package runtime;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.runtime.config.RuntimeConfig;
import net.magnesiumbackend.core.runtime.engine.CommandExecutor;
import net.magnesiumbackend.core.runtime.input.AmqpInputSource;
import net.magnesiumbackend.core.runtime.input.HttpInputSource;
import net.magnesiumbackend.core.runtime.input.InputSource;
import net.magnesiumbackend.core.runtime.kernel.RuntimeKernel;
import net.magnesiumbackend.core.runtime.lifecycle.LatchLifecyclePolicy;
import net.magnesiumbackend.core.runtime.lifecycle.ShellLifecyclePolicy;
import net.magnesiumbackend.shell.engine.ShellEngine;
import net.magnesiumbackend.shell.input.ShellInputSource;
import net.magnesiumbackend.shell.ir.CommandIR;

import java.util.HashMap;
import java.util.Map;

/**
 * Example demonstrating RuntimeKernel with multiple input sources.
 *
 * <p>This example shows how to compose a Magnesium application with:</p>
 * <ul>
 *   <li>ShellInputSource - Interactive CLI from stdin</li>
 *   <li>HttpInputSource - HTTP endpoint commands</li>
 *   <li>AmqpInputSource - Queue-based command processing</li>
 * </ul>
 */
public class RuntimeKernelExample {

    public static void main(String[] args) {
        // Build command registry
        Map<String, CommandIR> commands = buildCommands();

        // Create the shell engine as command executor
        ShellEngine shellEngine = new ShellEngine(buildHandlers()::get);
        CommandExecutor executor = shellEngine::execute;

        // Configuration 1: Shell-only mode (interactive CLI)
        if (args.length > 0 && args[0].equals("shell")) {
            runShellMode(commands, executor);
        }
        // Configuration 2: HTTP server mode
        else if (args.length > 0 && args[0].equals("http")) {
            runHttpMode(commands, executor);
        }
        // Configuration 3: Mixed mode (shell + HTTP + AMQP)
        else {
            runMixedMode(commands, executor);
        }
    }

    /**
     * Runs in shell-only mode. The shell owns the main thread.
     */
    static void runShellMode(Map<String, CommandIR> commands, CommandExecutor executor) {
        System.out.println("Starting Shell Mode...");

        RuntimeConfig config = RuntimeConfig.builder()
            .inputSource(new ShellInputSource(commands))
            .lifecyclePolicy(new ShellLifecyclePolicy())
            .build();

        RuntimeKernel kernel = new RuntimeKernel(config, executor);
        kernel.start(); // Blocks until user types 'exit'
    }

    /**
     * Runs in HTTP-only mode. Uses latch lifecycle.
     */
    static void runHttpMode(Map<String, CommandIR> commands, CommandExecutor executor) {
        System.out.println("Starting HTTP Mode on port 8080...");

        // Note: In real usage, you'd get these from your DI container
        // MagnesiumRuntime runtime = ...;
        // HttpRouteRegistry routes = ...;
        // MagnesiumTransport transport = ...;

        // For this example, we'd need actual HTTP transport instances
        // RuntimeConfig config = RuntimeConfig.builder()
        //     .inputSource(new HttpInputSource(8080, transport, runtime, routes))
        //     .lifecyclePolicy(new LatchLifecyclePolicy())
        //     .build();
        //
        // RuntimeKernel kernel = new RuntimeKernel(config, executor);
        // kernel.start();

        System.out.println("(HTTP mode requires configured transport - see full implementation)");
    }

    /**
     * Runs in mixed mode: shell for CLI + HTTP for API + AMQP for queue processing.
     */
    static void runMixedMode(Map<String, CommandIR> commands, CommandExecutor executor) {
        System.out.println("Starting Mixed Mode (Shell + HTTP + AMQP)...");

        LatchLifecyclePolicy lifecycle = new LatchLifecyclePolicy();

        // Build the configuration with multiple input sources
        RuntimeConfig.Builder builder = RuntimeConfig.builder()
            .inputSource(new ShellInputSource(commands, "multi> "))
            .lifecyclePolicy(lifecycle);

        // Add HTTP input source if available
        // builder.inputSource(new HttpInputSource(8080, transport, runtime, routes));

        // Add AMQP input source if RabbitMQ is configured
        // RabbitMQService rabbit = ...;
        // builder.inputSource(new AmqpInputSource(rabbit, "command-queue"));

        RuntimeConfig config = builder.build();
        RuntimeKernel kernel = new RuntimeKernel(config, executor);

        // Start kernel in a non-blocking way to demonstrate control
        Thread kernelThread = Thread.ofVirtual().start(kernel::start);

        System.out.println("Kernel started. Press Ctrl+C or call lifecycle.countDown() to stop.");

        // The latch blocks until countDown() is called
        // In production, this would be triggered by SIGTERM, admin API, etc.
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Command Registry Helpers
    // ═══════════════════════════════════════════════════════════════════════

    static Map<String, CommandIR> buildCommands() {
        Map<String, CommandIR> commands = new HashMap<>();

        // Define some example commands
        commands.put("echo", new CommandIR(
            "echo", "Echo text back", "1.0.0",
            new net.magnesiumbackend.shell.ir.ArgumentSchema.Builder()
                .addPositional("message", String.class, "Text to echo")
                .build(),
            net.magnesiumbackend.shell.ir.ExecutionMode.LOCAL
        ));

        commands.put("status", new CommandIR(
            "status", "Show system status", "1.0.0",
            new net.magnesiumbackend.shell.ir.ArgumentSchema.Builder().build(),
            net.magnesiumbackend.shell.ir.ExecutionMode.LOCAL
        ));

        commands.put("exit", new CommandIR(
            "exit", "Exit the shell", "1.0.0",
            new net.magnesiumbackend.shell.ir.ArgumentSchema.Builder().build(),
            net.magnesiumbackend.shell.ir.ExecutionMode.LOCAL
        ));

        return commands;
    }

    static Map<String, net.magnesiumbackend.shell.engine.GeneratedHandler> buildHandlers() {
        Map<String, net.magnesiumbackend.shell.engine.GeneratedHandler> handlers = new HashMap<>();

        handlers.put("echo", ctx -> {
            String message = ctx.arg("message");
            System.out.println(message);
        });

        handlers.put("status", ctx -> {
            Runtime runtime = Runtime.getRuntime();
            System.out.println("System Status:");
            System.out.println("  Free memory: " + (runtime.freeMemory() / 1024 / 1024) + " MB");
            System.out.println("  Total memory: " + (runtime.totalMemory() / 1024 / 1024) + " MB");
            System.out.println("  Available processors: " + runtime.availableProcessors());
        });

        handlers.put("exit", ctx -> {
            System.out.println("Goodbye!");
            System.exit(0);
        });

        return handlers;
    }
}
