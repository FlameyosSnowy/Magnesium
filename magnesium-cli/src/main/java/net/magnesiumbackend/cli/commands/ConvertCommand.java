package net.magnesiumbackend.cli.commands;

import net.magnesiumbackend.cli.MagnesiumCli;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot conversion command.
 *
 * <pre>{@code
 * magnesium-cli convert spring <path>
 * magnesium-cli convert spring . --assist
 * }</pre>
 */
public final class ConvertCommand implements MagnesiumCli.CliCommand {

    private final List<String> warnings = new ArrayList<>();
    private final List<String> converted = new ArrayList<>();
    private boolean assistMode = false;

    @Override
    public int execute(@NotNull String @NotNull [] args, @NotNull Path workingDir) {
        int length = args.length;
        if (length < 2 || !args[0].equals("spring")) {
            System.err.println("Usage: magnesium-cli convert spring <path> [--assist]");
            return 1;
        }

        Path targetPath = workingDir.resolve(args[1]).normalize();
        assistMode = false;

        for (int i = 2; i < length; i++) {
            if (args[i].equals("--assist")) {
                assistMode = true;
                break;
            }
        }

        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.CYAN)
            .a("Converting Spring Boot project at: " + targetPath)
            .reset()
        );

        if (!Files.exists(targetPath)) {
            System.err.println("Path does not exist: " + targetPath);
            return 1;
        }

        try {
            convertProject(targetPath);
            printSummary();
            return 0;
        } catch (IOException e) {
            System.err.println("Conversion failed: " + e.getMessage());
            return 1;
        }
    }

    private void convertProject(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();

                if (fileName.endsWith(".java")) {
                    try {
                        convertJavaFile(file);
                    } catch (IOException e) {
                        warnings.add("Failed to convert: " + file);
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        });

        // Convert build file
        convertBuildFile(path);
    }

    private void convertJavaFile(Path file) throws IOException {
        String content = Files.readString(file);
        String original = content;

        // Convert annotations
        content = content.replace("@RequestMapping", "@Path");
        content = content.replace("@Service", "@RestService");
        content = content.replace("@Component", "@RestService");

        // Convert imports
        content = content.replace("import org.springframework.web.bind.annotation.", "import net.magnesiumbackend.core.annotations.");
        content = content.replace("import org.springframework.stereotype.Service", "import net.magnesiumbackend.core.annotations.RestService");

        // Detect problematic patterns
        if (original.contains("Transactional")) {
            warnings.add(file + ": @Transactional requires manual rewrite (transaction management differs)");
        }

        if (original.contains("JpaRepository") || original.contains("CrudRepository")) {
            warnings.add(file + ": JPA repositories require manual migration (use @Repository with @Query)");
        }

        if (original.contains("Entity")) {
            warnings.add(file + ": JPA entities require manual migration (Magnesium uses Universal Mapper)");
        }

        if (original.contains("SpringSecurity")) {
            warnings.add(file + ": Spring Security configuration requires manual rewrite");
        }

        // Check for automatic conversion
        if (!content.equals(original)) {
            if (!assistMode) {
                Files.writeString(file, content);
                converted.add(file.toString());
            } else {
                // In assist mode, create .magnesium/assist suggestions
                createAssistFile(file, original, content);
            }
        }
    }

    private void convertBuildFile(Path projectPath) throws IOException {
        Path pom = projectPath.resolve("pom.xml");
        Path buildGradle = projectPath.resolve("build.gradle");

        if (Files.exists(pom)) {
            String content = Files.readString(pom);

            // Replace Spring Boot parent with Magnesium
            content = content.replaceAll(
                "<parent>.*?<groupId>org.springframework.boot</groupId>.*?</parent>",
                """
                <parent>
                    <groupId>net.magnesiumbackend</groupId>
                    <artifactId>magnesium-parent</artifactId>
                    <version>1.0.0</version>
                </parent>
                """
            );

            // Replace Spring dependencies with Magnesium
            content = content.replaceAll(
                "<dependency>\\s*<groupId>org.springframework.boot</groupId>.*?<artifactId>spring-boot-starter-web</artifactId>.*?</dependency>",
                """
                <dependency>
                    <groupId>net.magnesiumbackend</groupId>
                    <artifactId>magnesium-transport-tomcat</artifactId>
                </dependency>
                `"""
            );

            if (!assistMode) {
                Files.writeString(pom, content);
                converted.add("pom.xml (build configuration)");
            }
        }

        if (Files.exists(buildGradle)) {
            warnings.add("build.gradle: Manual conversion required (see docs)");
        }
    }

    private void createAssistFile(Path original, String before, String after) throws IOException {
        Path assistDir = original.getParent().resolve(".magnesium/assist");
        Files.createDirectories(assistDir);

        String diff = """
            # Conversion Suggestion for: %s

            ## Original (Spring Boot)
            ```java
            %s
            ```

            ## Suggested (Magnesium)
            ```java
            %s
            ```

            ## Manual Tasks
            - [ ] Verify imports
            - [ ] Test endpoints
            - [ ] Update configuration
            """.formatted(original.getFileName(), before, after);

        Files.writeString(assistDir.resolve(original.getFileName() + ".md"), diff);
    }

    private void printSummary() {
        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.YELLOW)
            .a("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            .reset()
        );

        if (assistMode) {
            System.out.println(Ansi.ansi()
                .fg(Ansi.Color.BLUE)
                .a("Assist mode enabled - suggestions written to .magnesium/assist/")
                .reset()
            );
        }

        System.out.println(Ansi.ansi().fg(Ansi.Color.GREEN).a("Converted:").reset());
        for (String item : converted) {
            System.out.println("  ✔ " + item);
        }

        if (!warnings.isEmpty()) {
            System.out.println(Ansi.ansi()
                .fg(Ansi.Color.YELLOW)
                .a("\nRequires manual attention:")
                .reset()
            );
            for (String warning : warnings) {
                System.out.println("  ⚠ " + warning);
            }
        }

        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.YELLOW)
            .a("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            .reset()
        );
    }

    @Override
    public @NotNull String description() {
        return "Convert Spring Boot project to Magnesium (spring <path> [--assist])";
    }
}
