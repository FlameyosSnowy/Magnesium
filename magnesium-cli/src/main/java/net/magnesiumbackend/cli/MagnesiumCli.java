package net.magnesiumbackend.cli;

import net.magnesiumbackend.cli.commands.*;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Magnesium CLI - Build-time tooling for Magnesium applications.
 *
 * <p>Split responsibility with magnesium-shell:</p>
 * <ul>
 *   <li><b>CLI</b> - Project lifecycle, build, scaffolding (this tool)</li>
 *   <li><b>Shell</b> - Runtime control and inspection (embedded in apps)</li>
 * </ul>
 *
 * <h3>Commands</h3>
 * <ul>
 *   <li>{@code new} - Interactive project creation</li>
 *   <li>{@code run} - Run application with shell attached</li>
 *   <li>{@code dev} - Dev mode with hot reload</li>
 *   <li>{@code build} - Build and package</li>
 *   <li>{@code generate} - Code generation (service, route, etc.)</li>
 *   <li>{@code convert} - Spring Boot conversion</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * magnesium-cli new                    # Create new project interactively
 * magnesium-cli run                    # Run the application
 * magnesium-cli build --native         # Build native image
 * magnesium-cli generate service User  # Generate a service class
 * }</pre>
 */
public final class MagnesiumCli {

    private static final Logger logger = LoggerFactory.getLogger(MagnesiumCli.class);

    private final Map<String, CliCommand> commands = new HashMap<>();
    private final Path workingDir;

    public MagnesiumCli(@NotNull Path workingDir) {
        this.workingDir = workingDir;
        registerCommands();
    }

    public static void main(String[] args) {
        AnsiConsole.systemInstall();

        try {
            Path workingDir = Paths.get(".").toAbsolutePath().normalize();
            MagnesiumCli cli = new MagnesiumCli(workingDir);

            if (args.length == 0) {
                cli.showHelp();
                System.exit(0);
            }

            int exitCode = cli.execute(args);
            System.exit(exitCode);

        } catch (Exception e) {
            logger.error("CLI error", e);
            logger.error(String.valueOf(Ansi.ansi()
                .fg(Ansi.Color.RED)
                .a("Error: ")
                .reset()
                .a(e.getMessage()))
            );
            System.exit(1);
        } finally {
            AnsiConsole.systemUninstall();
        }
    }

    private void registerCommands() {
        commands.put("new", new NewCommand());
        commands.put("run", new RunCommand());
        commands.put("dev", new DevCommand());
        commands.put("build", new BuildCommand());
        commands.put("generate", new GenerateCommand());
        commands.put("convert", new ConvertCommand());
        commands.put("help", new HelpCommand(commands));
    }

    public int execute(@NotNull String @NotNull [] args) {
        String commandName = args[0];
        CliCommand command = commands.get(commandName);

        if (command == null) {
            logger.error(String.valueOf(Ansi.ansi()
                .fg(Ansi.Color.RED)
                .a("Unknown command: ")
                .reset()
                .a(commandName))
            );
            logger.error("Run 'magnesium-cli help' for available commands.");
            return 1;
        }

        String[] commandArgs = args.length > 1
            ? Arrays.copyOfRange(args, 1, args.length)
            : new String[0];

        return command.execute(commandArgs, workingDir);
    }

    private void showHelp() {
        commands.get("help").execute(new String[0], workingDir);
    }

    @FunctionalInterface
    public interface CliCommand {
        /**
         * Executes the command.
         *
         * @param args command arguments
         * @param workingDir current working directory
         * @return exit code (0 for success)
         */
        int execute(@NotNull String @NotNull [] args, @NotNull Path workingDir);

        /**
         * Returns command description.
         *
         * @return description string
         */
        default @NotNull String description() {
            return "No description available";
        }
    }
}
