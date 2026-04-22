package net.magnesiumbackend.cli.commands;

import net.magnesiumbackend.cli.MagnesiumCli;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Code generation command.
 *
 * <pre>{@code
 * magnesium-cli generate service User
 * magnesium-cli generate route /users
 * magnesium-cli generate command Backup
 * }</pre>
 */
public final class GenerateCommand implements MagnesiumCli.CliCommand {

    @Override
    public int execute(@NotNull String @NotNull [] args, @NotNull Path workingDir) {
        if (args.length < 2) {
            System.err.println("Usage: magnesium-cli generate <type> <name>");
            System.err.println("Types: service, repository, route, command");
            return 1;
        }

        String type = args[0].toLowerCase();
        String name = args[1];

        // Determine package
        String packageName = detectPackage(workingDir);

        Path srcDir = workingDir.resolve("src/main/java");
        Path packageDir = srcDir.resolve(packageName.replace('.', '/'));

        try {
            switch (type) {
                case "service" -> generateService(packageDir, packageName, name);
                case "repository" -> generateRepository(packageDir, packageName, name);
                case "route" -> generateRoute(packageDir, packageName, name);
                case "command" -> generateCommand(packageDir, packageName, name);
                default -> {
                    System.err.println("Unknown type: " + type);
                    return 1;
                }
            }

            System.out.println(Ansi.ansi()
                .fg(Ansi.Color.GREEN)
                .a("✓ Generated " + type + ": " + name)
                .reset()
            );

            return 0;
        } catch (IOException e) {
            System.err.println("Failed to generate: " + e.getMessage());
            return 1;
        }
    }

    private void generateService(Path packageDir, String packageName, String name) throws IOException {
        String className = capitalize(name) + "Service";

        String content = """
            package %s;

            import net.magnesiumbackend.core.annotations.service.Service;

            @Service
            public class %s {

                public void doSomething() {
                    // TODO: Implement
                }
            }
            """.formatted(packageName, className);

        Files.createDirectories(packageDir.resolve("service"));
        Files.writeString(packageDir.resolve("service/" + className + ".java"), content);
    }

    private void generateRepository(Path packageDir, String packageName, String name) throws IOException {
        String entityName = capitalize(name);
        String className = entityName + "Repository";

        String content = """
            package %s.repository;

            import net.magnesiumbackend.core.annotations.data.Repository;
            import net.magnesiumbackend.core.annotations.data.Query;
            import java.util.List;
            import java.util.Optional;

            @Repository
            public interface %s {

                @Query("SELECT * FROM %s WHERE id = ?")
                Optional<%s> findById(Long id);

                @Query("SELECT * FROM %s")
                List<%s> findAll();

                @Query("INSERT INTO %s (id, name) VALUES (?, ?)")
                void save(%s entity);
            }
            """.formatted(packageName, className,
                name.toLowerCase(), entityName,
                name.toLowerCase(), entityName,
                name.toLowerCase(), entityName);

        Files.createDirectories(packageDir.resolve("repository"));
        Files.writeString(packageDir.resolve("repository/" + className + ".java"), content);
    }

    private void generateRoute(Path packageDir, String packageName, String path) throws IOException {
        String className = "Route" + capitalize(sanitize(path));

        String content = """
            package %s.route;

            import net.magnesiumbackend.core.annotations.route.*;
            import net.magnesiumbackend.core.http.Response;

            public class %s {

                @Get("%s")
                public Response get() {
                    return Response.ok().body("Hello from %s");
                }

                @Post("%s")
                public Response post(@Body String body) {
                    return Response.created().body(body);
                }
            }
            """.formatted(packageName, className, path, path, path);

        Files.createDirectories(packageDir.resolve("route"));
        Files.writeString(packageDir.resolve("route/" + className + ".java"), content);
    }

    private void generateCommand(Path packageDir, String packageName, String name) throws IOException {
        String className = capitalize(name) + "Command";

        String content = """
            package %s.command;

            import net.magnesiumbackend.shell.annotation.Command;
            import net.magnesiumbackend.shell.dsl.CommandContext;

            @Command("%s")
            public class %s {

                public void execute(CommandContext ctx) {
                    System.out.println("Executing %s command");
                    // TODO: Implement command logic
                }
            }
            """.formatted(packageName, name.toLowerCase(), className, name);

        Files.createDirectories(packageDir.resolve("command"));
        Files.writeString(packageDir.resolve("command/" + className + ".java"), content);
    }

    private String detectPackage(Path workingDir) {
        // Try to detect from build file
        Path pom = workingDir.resolve("pom.xml");
        if (Files.exists(pom)) {
            try {
                String content = Files.readString(pom);
                int groupStart = content.indexOf("<groupId>") + 9;
                int groupEnd = content.indexOf("</groupId>", groupStart);
                if (groupEnd > groupStart) {
                    return content.substring(groupStart, groupEnd).trim();
                }
            } catch (IOException ignored) {}
        }

        return "com.example.app";
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9]", "");
    }

    @Override
    public @NotNull String description() {
        return "Generate code (service, repository, route, command)";
    }
}
