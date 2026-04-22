package examples.shell;

import net.magnesiumbackend.shell.annotation.Arg;
import net.magnesiumbackend.shell.annotation.Command;
import net.magnesiumbackend.shell.dsl.CommandBuilder;
import net.magnesiumbackend.shell.dsl.CommandContext;
import net.magnesiumbackend.shell.engine.GeneratedHandler;
import net.magnesiumbackend.shell.engine.ShellEngine;
import net.magnesiumbackend.shell.help.HelpGenerator;
import net.magnesiumbackend.shell.ir.CommandIR;

import java.util.HashMap;
import java.util.Map;

/**
 * Example demonstrating the Magnesium Shell framework.
 *
 * <p>Shows both annotation-based and DSL-based command definitions.</p>
 *
 * <h3>Run this example</h3>
 * <pre>{@code
 * mvn compile exec:java -Dexec.mainClass=examples.shell.ShellExample
 * }</pre>
 */
public class ShellExample {

    // ═══════════════════════════════════════════════════════════════════════
    // ANNOTATION-BASED COMMANDS
    // ═══════════════════════════════════════════════════════════════════════

    @Command(name = "user:create", description = "Create a new user")
    static class CreateUserCommand {
        @Arg(required = true, description = "User email address")
        String email;

        @Arg(defaultValue = "USER", description = "User role")
        String role;

        @Arg(flag = true, description = "Grant admin privileges")
        boolean admin;

        void run() {
            System.out.println("Created user: " + email);
            System.out.println("Role: " + role);
            System.out.println("Admin: " + admin);
        }
    }

    @Command(name = "user:delete", description = "Delete a user")
    static class DeleteUserCommand {
        @Arg(required = true, positional = true, description = "User ID to delete")
        String userId;

        @Arg(flag = true, description = "Force deletion without confirmation")
        boolean force;

        void run() {
            if (!force) {
                System.out.println("Use --force to confirm deletion of user: " + userId);
                return;
            }
            System.out.println("Deleted user: " + userId);
        }
    }

    @Command(name = "config:get", description = "Get configuration value")
    static class ConfigGetCommand {
        @Arg(required = true, positional = true, description = "Configuration key")
        String key;

        void run() {
            String value = System.getProperty(key, "<not set>");
            System.out.println(key + " = " + value);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DSL-BASED COMMANDS
    // ═══════════════════════════════════════════════════════════════════════

    static CommandIR createListUsersCommand() {
        return CommandBuilder.create("user:list")
            .description("List all users")
            .arg("limit", Integer.class)
                .defaultValue(10)
                .description("Maximum number of users to show")
                .add()
            .arg("verbose", boolean.class)
                .flag()
                .description("Show detailed information")
                .add()
            .buildIR("examples.shell.ListUsersHandler", "execute");
    }

    static class ListUsersHandler implements GeneratedHandler {
        @Override
        public void execute(CommandContext ctx) {
            int limit = ctx.argOrDefault("limit", 10);
            boolean verbose = ctx.flag("verbose");

            System.out.println("Showing up to " + limit + " users");
            if (verbose) {
                System.out.println("(verbose mode enabled)");
            }

            // Simulated user list
            for (int i = 1; i <= Math.min(limit, 5); i++) {
                System.out.println("  - user" + i + "@example.com");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║       Magnesium Shell Framework - Example                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // Build command registry manually (in real use, this is generated)
        Map<String, GeneratedHandler> handlers = new HashMap<>();
        Map<String, CommandIR> commands = new HashMap<>();

        // Register annotation-based commands (normally auto-generated)
        handlers.put("user:create", new CreateUserCommandHandler());
        handlers.put("user:delete", new DeleteUserCommandHandler());
        handlers.put("config:get", new ConfigGetCommandHandler());

        // Register DSL-based commands
        CommandIR listUsersIR = createListUsersCommand();
        commands.put("user:list", listUsersIR);
        handlers.put("user:list", new ListUsersHandler());

        // Create engine
        ShellEngine engine = new ShellEngine(handlers::get);

        // Start interactive mode by default (reads from stdin)
        // Supports: tab completion, help, history
        engine.startInteractive(commands);
    }

    // Handlers would normally be generated by annotation processor
    static class CreateUserCommandHandler implements GeneratedHandler {
        @Override
        public void execute(CommandContext ctx) {
            CreateUserCommand cmd = new CreateUserCommand();
            cmd.email = ctx.arg("email");
            cmd.role = ctx.argOrDefault("role", "USER");
            cmd.admin = ctx.flag("admin");
            cmd.run();
        }
    }

    static class DeleteUserCommandHandler implements GeneratedHandler {
        @Override
        public void execute(CommandContext ctx) {
            DeleteUserCommand cmd = new DeleteUserCommand();
            cmd.userId = ctx.arg("userId");
            cmd.force = ctx.flag("force");
            cmd.run();
        }
    }

    static class ConfigGetCommandHandler implements GeneratedHandler {
        @Override
        public void execute(CommandContext ctx) {
            ConfigGetCommand cmd = new ConfigGetCommand();
            cmd.key = ctx.arg("key");
            cmd.run();
        }
    }
}
