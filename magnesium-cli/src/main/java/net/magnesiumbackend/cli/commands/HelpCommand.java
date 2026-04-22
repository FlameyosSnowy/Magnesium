package net.magnesiumbackend.cli.commands;

import net.magnesiumbackend.cli.MagnesiumCli;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

/**
 * Help command - displays usage information.
 */
public final class HelpCommand implements MagnesiumCli.CliCommand {

    private final Map<String, MagnesiumCli.CliCommand> commands;

    public HelpCommand(Map<String, MagnesiumCli.CliCommand> commands) {
        this.commands = commands;
    }

    @Override
    public int execute(@NotNull String @NotNull [] args, @NotNull Path workingDir) {
        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.CYAN)
            .a("╔════════════════════════════════════════════════════════════╗\n")
            .a("║              Magnesium CLI - Build-time Tooling              ║\n")
            .a("╚════════════════════════════════════════════════════════════╝")
            .reset()
        );

        System.out.println("\nUsage: magnesium-cli <command> [options]\n");
        System.out.println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("Commands:").reset());

        int maxLen = commands.keySet().stream()
            .mapToInt(String::length)
            .max()
            .orElse(10);

        for (Map.Entry<String, MagnesiumCli.CliCommand> entry : commands.entrySet()) {
            String padded = String.format("  %%-%ds", maxLen + 2).formatted(entry.getKey());
            System.out.println(Ansi.ansi()
                .fg(Ansi.Color.GREEN)
                .a(padded)
                .reset()
                .a(entry.getValue().description())
            );
        }

        System.out.println("""

            Examples:
              magnesium-cli new                    # Create new project
              magnesium-cli run                    # Run application
              magnesium-cli dev                    # Dev mode with hot reload
              magnesium-cli build --native         # Build native image
              magnesium-cli generate service User  # Generate service class
              magnesium-cli convert spring .       # Convert Spring Boot project
            """);

        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.CYAN)
            .a("For more info: https://magnesium.dev/docs/cli")
            .reset()
        );

        return 0;
    }

    @Override
    public @NotNull String description() {
        return "Show help information";
    }
}
