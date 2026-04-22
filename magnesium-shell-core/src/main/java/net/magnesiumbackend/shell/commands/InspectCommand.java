package net.magnesiumbackend.shell.commands;

import net.magnesiumbackend.shell.annotation.Command;
import net.magnesiumbackend.shell.dsl.CommandContext;
import net.magnesiumbackend.shell.ir.ExecutionMode;

/**
 * Runtime inspection command.
 *
 * <pre>{@code
 * > inspect
 * > inspect.routes
 * > inspect.services
 * > inspect.config
 * }</pre>
 */
@Command(value = "inspect", mode = ExecutionMode.LOCAL)
public final class InspectCommand {

    public void execute(CommandContext ctx) {
        String subcommand = ctx.argOrDefault("subcommand", "all");

        switch (subcommand.toLowerCase()) {
            case "routes" -> showRoutes();
            case "services" -> showServices();
            case "config" -> showConfig();
            case "modules" -> showModules();
            default -> showAll();
        }
    }

    private void showAll() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║               Runtime Inspection                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        showModules();
        System.out.println();
        showRoutes();
        System.out.println();
        showServices();
    }

    private void showModules() {
        System.out.println("📦 Loaded Modules:");
        System.out.println("─────────────────────────────────────────────────────────────");

        // In a real implementation, this would query the runtime
        System.out.println("  • core              - Core framework");
        System.out.println("  • web               - HTTP transport");
        System.out.println("  • data              - Database access");
        System.out.println("  • actuator          - Health & metrics");
    }

    private void showRoutes() {
        System.out.println("🌐 Registered Routes:");
        System.out.println("─────────────────────────────────────────────────────────────");

        // In a real implementation, this would query the route registry
        System.out.println("  GET    /             → MainApplication.home()");
        System.out.println("  GET    /health       → ActuatorController.health()");
        System.out.println("  GET    /metrics      → ActuatorController.metrics()");
    }

    private void showServices() {
        System.out.println("🔧 Available Services:");
        System.out.println("─────────────────────────────────────────────────────────────");

        // In a real implementation, this would query the service registry
        System.out.println("  • HealthIndicator     [running]");
        System.out.println("  • MetricsRegistry     [running]");
        System.out.println("  • DatabaseService     [running]");
    }

    private void showConfig() {
        System.out.println("⚙️  Configuration:");
        System.out.println("─────────────────────────────────────────────────────────────");

        // In a real implementation, this would query the config
        System.out.println("  server.port = 8080");
        System.out.println("  logging.level = INFO");
        System.out.println("  data.url = jdbc:postgresql://localhost:5432/app");
    }
}
