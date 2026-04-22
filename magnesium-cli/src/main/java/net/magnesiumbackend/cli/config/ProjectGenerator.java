package net.magnesiumbackend.cli.config;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates a new Magnesium project structure.
 */
public final class ProjectGenerator {

    private final String projectName;
    private final String groupId;
    private final String artifactId;
    private final List<String> modules;
    private final List<String> dependencies;
    private final String dataSubmodule;
    private final String buildTool;
    private final String configFormat;
    private final boolean devTools;
    private final Path projectDir;

    public ProjectGenerator(
        @NotNull String projectName,
        @NotNull String groupId,
        @NotNull String artifactId,
        @NotNull List<String> modules,
        @NotNull List<String> dependencies,
        @NotNull String dataSubmodule,
        @NotNull String buildTool,
        @NotNull String configFormat,
        boolean devTools,
        @NotNull Path projectDir
    ) {
        this.projectName = projectName;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.modules = modules;
        this.dependencies = dependencies;
        this.dataSubmodule = dataSubmodule;
        this.buildTool = buildTool;
        this.configFormat = configFormat;
        this.devTools = devTools;
        this.projectDir = projectDir;
    }

    public void generate() throws IOException {
        // Create directory structure
        Path srcMainJava = projectDir.resolve("src/main/java");
        Path srcMainResources = projectDir.resolve("src/main/resources");
        Path magnesiumDir = projectDir.resolve("magnesium/modules");

        Files.createDirectories(srcMainJava);
        Files.createDirectories(srcMainResources);
        Files.createDirectories(magnesiumDir);

        // Generate build file
        if (buildTool.equals("maven")) {
            generatePom();
        } else {
            generateBuildGradle();
        }

        // Generate config file
        generateConfig(srcMainResources);

        // Generate main application class
        generateMainClass(srcMainJava);

        // Generate module configs in magnesium/ directory
        generateModuleConfigs(magnesiumDir);
    }

    private void generatePom() throws IOException {
        StringBuilder deps = new StringBuilder();

        // Core
        deps.append("""
            <dependency>
                <groupId>net.magnesiumbackend</groupId>
                <artifactId>core</artifactId>
                <version>1.0.0</version>
            </dependency>
            """);

        // JSON Provider (required for serialization)
        deps.append("""
            <dependency>
                <groupId>net.magnesiumbackend</groupId>
                <artifactId>magnesium-jackson-json-provider</artifactId>
                <version>1.0.0</version>
            </dependency>
            """);

        // Annotation Processor (for @RestController code generation)
        deps.append("""
            <dependency>
                <groupId>net.magnesiumbackend</groupId>
                <artifactId>annotation-processor</artifactId>
                <version>1.0.0</version>
                <scope>provided</scope>
            </dependency>
            """);

        // Modules
        if (hasModule("Web")) {
            deps.append("""
            <dependency>
                <groupId>net.magnesiumbackend</groupId>
                <artifactId>magnesium-transport-tomcat</artifactId>
                <version>1.0.0</version>
            </dependency>
            """);
        }

        if (hasModule("Data")) {
            if (dataSubmodule.equals("jdbc")) {
                deps.append("""
            <dependency>
                <groupId>net.magnesiumbackend</groupId>
                <artifactId>magnesium-data-jdbc</artifactId>
                <version>1.0.0</version>
            </dependency>
            """);
            }
            if (dataSubmodule.equals("redis") || dataSubmodule.equals("both")) {
                deps.append("""
            <dependency>
                <groupId>net.magnesiumbackend</groupId>
                <artifactId>magnesium-data-redis</artifactId>
                <version>1.0.0</version>
            </dependency>
            """);
            }
        }

        if (hasModule("AMQP")) {
            deps.append("""
            <dependency>
                <groupId>net.magnesiumbackend</groupId>
                <artifactId>magnesium-amqp-rabbitmq</artifactId>
                <version>1.0.0</version>
            </dependency>
            """);
        }

        if (hasModule("Actuator")) {
            deps.append("""
            <dependency>
                <groupId>net.magnesiumbackend</groupId>
                <artifactId>magnesium-actuator</artifactId>
                <version>1.0.0</version>
            </dependency>
            """);
        }

        if (devTools) {
            deps.append("""
            <dependency>
                <groupId>net.magnesiumbackend</groupId>
                <artifactId>magnesium-devtools</artifactId>
                <version>1.0.0</version>
            </dependency>
            """);
        }

        String pom = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <packaging>jar</packaging>
                <name>%s</name>

                <properties>
                    <maven.compiler.source>21</maven.compiler.source>
                    <maven.compiler.target>21</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>

                <dependencies>
                    %s
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.13.0</version>
                            <configuration>
                                <annotationProcessorPaths>
                                    <path>
                                        <groupId>net.magnesiumbackend</groupId>
                                        <artifactId>annotation-processor</artifactId>
                                        <version>1.0.0</version>
                                    </path>
                                </annotationProcessorPaths>
                            </configuration>
                        </plugin>
                        <plugin>
                            <groupId>org.codehaus.mojo</groupId>
                            <artifactId>exec-maven-plugin</artifactId>
                            <version>3.2.0</version>
                            <configuration>
                                <mainClass>%s.MainApplication</mainClass>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """.formatted(groupId, artifactId, projectName, deps.toString(), groupId);

        Files.writeString(projectDir.resolve("pom.xml"), pom);
    }

    private void generateBuildGradle() throws IOException {
        StringBuilder deps = new StringBuilder();

        deps.append("    implementation 'net.magnesiumbackend:core:1.0.0'\n");
        deps.append("    implementation 'net.magnesiumbackend:magnesium-jackson-json-provider:1.0.0'\n");
        deps.append("    annotationProcessor 'net.magnesiumbackend:annotation-processor:1.0.0'\n");

        if (hasModule("Web")) {
            deps.append("    implementation 'net.magnesiumbackend:magnesium-transport-tomcat:1.0.0'\n");
        }
        if (hasModule("Data")) {
            if (dataSubmodule.equals("jdbc")) {
                deps.append("    implementation 'net.magnesiumbackend:magnesium-data-jdbc:1.0.0'\n");
            }
            if (dataSubmodule.contains("redis")) {
                deps.append("    implementation 'net.magnesiumbackend:magnesium-data-redis:1.0.0'\n");
            }
        }
        if (hasModule("AMQP")) {
            deps.append("    implementation 'net.magnesiumbackend:magnesium-amqp-rabbitmq:1.0.0'\n");
        }
        if (hasModule("Actuator")) {
            deps.append("    implementation 'net.magnesiumbackend:magnesium-actuator:1.0.0'\n");
        }
        if (devTools) {
            deps.append("    implementation 'net.magnesiumbackend:magnesium-devtools:1.0.0'\n");
        }

        String build = """
            plugins {
                id 'java'
                id 'application'
            }

            group = '%s'
            version = '1.0.0-SNAPSHOT'

            java {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }

            repositories {
                mavenCentral()
                mavenLocal()
            }

            dependencies {
            %s}

            application {
                mainClass = '%s.MainApplication'
            }
            """.formatted(groupId, deps.toString(), groupId);

        Files.writeString(projectDir.resolve("build.gradle"), build);
    }

    private void generateConfig(Path resourcesDir) throws IOException {
        String ext = configFormat.replace("application.", "");
        Path configFile = resourcesDir.resolve("application." + ext);

        String config = """
            [server]
            port = 8080

            [logging]
            level = "INFO"
            """;

        if (hasModule("Data")) {
            config += """

            [data]
            url = "jdbc:postgresql://localhost:5432/%s"
            user = "postgres"
            password = "password"
            """.formatted(projectName.replace("-", "_"));
        }

        if (hasModule("AMQP")) {
            config += """

            [amqp]
            host = "localhost"
            port = 5672
            username = "guest"
            password = "guest"
            """;
        }

        Files.writeString(configFile, config);
    }

    private void generateMainClass(Path javaDir) throws IOException {
        String packagePath = groupId.replace('.', '/');
        Path packageDir = javaDir.resolve(packagePath);
        Files.createDirectories(packageDir);

        String imports = """
            import net.magnesiumbackend.core.Application;
            import net.magnesiumbackend.core.MagnesiumApplication;
            import net.magnesiumbackend.core.MagnesiumRuntime;
            import net.magnesiumbackend.core.http.response.Response;
            """;

        String configureBody = """
                // Programmatic routes
                runtime.router()
                    .get("/", ctx -> Response.ok("Hello from %s!"));
            """.formatted(projectName);

        if (hasModule("Web")) {
            imports += """
            import net.magnesiumbackend.core.annotations.GetMapping;
            import net.magnesiumbackend.core.annotations.RestController;
            import net.magnesiumbackend.core.http.response.ResponseEntity;
            """;
            // Routes are auto-discovered via annotation processor - no manual registration needed
            configureBody = """
                // Routes are auto-discovered from @RestController classes at compile time
            """;
        }

        String controllerMethods = "";
        if (hasModule("Web")) {
            controllerMethods = """

    // Example endpoint - returns plain text
    @GetMapping(path = "/")
    public String home() {
        return "Hello from %s!";
    }

    // Example endpoint - returns JSON
    @GetMapping(path = "/api/status")
    public ResponseEntity<java.util.Map<String, String>> status() {
        return ResponseEntity.ok(java.util.Map.of(
            "status", "UP",
            "application", "%s",
            "version", "1.0.0"
        ));
    }
""".formatted(projectName, projectName);
        }

        String restControllerAnnotation = hasModule("Web") ? "@RestController\n" : "";

        String mainClass = """
            package %s;

            %s
            %spublic class MainApplication extends Application {

                @Override
                protected void configure(MagnesiumRuntime runtime) {
            %s
                }
            %s
                public static void main(String[] args) {
                    MagnesiumApplication.run(new MainApplication(), 8080);
                }
            }
            """.formatted(groupId, imports.stripTrailing(), restControllerAnnotation, configureBody.indent(8).stripTrailing(), controllerMethods);

        Files.writeString(packageDir.resolve("MainApplication.java"), mainClass);
    }

    private void generateModuleConfigs(Path magnesiumDir) throws IOException {
        // Placeholder for module-specific configuration
        Files.writeString(magnesiumDir.resolve(".gitkeep"), "");
    }

    private boolean hasModule(String name) {
        return modules.stream().anyMatch(m -> m.toLowerCase().contains(name.toLowerCase()));
    }
}
