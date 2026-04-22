package net.magnesiumbackend.cli.commands;

import net.magnesiumbackend.cli.MagnesiumCli;
import net.magnesiumbackend.cli.interactive.InteractivePrompt;
import net.magnesiumbackend.cli.config.ProjectGenerator;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Interactive project creation command.
 *
 * <pre>{@code
 * magnesium-cli new
 * }</pre>
 */
public final class NewCommand implements MagnesiumCli.CliCommand {

    private static final Pattern VALID_NAME = Pattern.compile("^[a-z][a-z0-9-]*$");
    private static final Pattern VALID_GROUP = Pattern.compile("^[a-z][a-z0-9.]*$");
    private static final Logger log = LoggerFactory.getLogger(NewCommand.class);

    @Override
    public int execute(@NotNull String @NotNull [] args, @NotNull Path workingDir) {
        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.CYAN)
            .a("╔════════════════════════════════════════════════════════════╗\n")
            .a("║         Magnesium Project Generator                        ║\n")
            .a("╚════════════════════════════════════════════════════════════╝")
            .reset()
        );

        InteractivePrompt prompt = new InteractivePrompt();

        try {
            // Project basics
            String projectName = prompt.prompt(
                "Project name",
                null,
                s -> VALID_NAME.matcher(s).matches()
            );

            String groupId = prompt.prompt(
                "Group ID",
                "com.example",
                s -> VALID_GROUP.matcher(s).matches()
            );

            String artifactId = prompt.prompt(
                "Artifact ID",
                projectName,
                s -> !s.isEmpty()
            );

            // Module selection
            List<String> availableModules = List.of(
                "Web (HTTP server)",
                "Data (JDBC/Database)",
                "AMQP (RabbitMQ)",
                "Security",
                "Actuator (Health/Metrics)",
                "Redis Cache",
                "Elasticsearch"
            );

            boolean[] moduleDefaults = new boolean[availableModules.size()];
            moduleDefaults[0] = true; // Web selected by default
            moduleDefaults[1] = true; // Data selected by default

            List<String> selectedModules = prompt.checklist(
                "Select modules",
                availableModules,
                moduleDefaults
            );

            // Data submodules (if Data selected)
            String dataSubmodule = "jdbc";
            if (selectedModules.stream().anyMatch(s -> s.contains("Data"))) {
                List<String> dataOptions = List.of("JDBC", "Redis", "Both");
                String dataChoice = prompt.radio(
                    "Data implementation",
                    dataOptions,
                    0
                );
                dataSubmodule = dataChoice.toLowerCase().contains("redis") ? "redis" : "jdbc";
            }

            // Dependencies
            List<String> availableDeps = List.of(
                "Hibernate",
                "Universal Mapper"
            );

            List<String> selectedDeps = prompt.checklist(
                "Additional dependencies",
                availableDeps,
                new boolean[availableDeps.size()]
            );

            // Build tool
            String buildTool = prompt.radio(
                "Build tool",
                List.of("Maven", "Gradle"),
                0
            ).toLowerCase();

            // Config format
            String configFormat = prompt.radio(
                "Config format",
                List.of("application.toml", "application.yml", "application.properties"),
                0
            );

            // DevTools
            boolean devTools = prompt.confirm("Enable DevTools (hot reload)?", true);

            // Summary
            System.out.println(Ansi.ansi()
                .fg(Ansi.Color.YELLOW)
                .a("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .reset());

            System.out.println("Project: " + projectName);
            System.out.println("Group:   " + groupId);
            System.out.println("Artifact: " + artifactId);
            System.out.println("Modules: " + String.join(", ", selectedModules));
            System.out.println("Build:   " + buildTool);
            System.out.println("Config:  " + configFormat);

            if (!prompt.confirm("\nCreate project?", true)) {
                System.out.println("Cancelled.");
                return 0;
            }

            // Generate project
            Path projectDir = workingDir.resolve(projectName);
            if (Files.exists(projectDir)) {
                prompt.error("Directory already exists: " + projectName);
                return 1;
            }

            ProjectGenerator generator = new ProjectGenerator(
                projectName,
                groupId,
                artifactId,
                selectedModules,
                selectedDeps,
                dataSubmodule,
                buildTool,
                configFormat,
                devTools,
                projectDir
            );

            generator.generate();

            prompt.success("Project created at: " + projectDir);
            System.out.println("\nNext steps:");
            System.out.println("  cd " + projectName);
            System.out.println("  magnesium-cli run");

            return 0;

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    @Override
    public @NotNull String description() {
        return "Create a new Magnesium project interactively";
    }
}
