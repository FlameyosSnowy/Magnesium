package net.magnesiumbackend.core.test;

import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.http.MagnesiumTransport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Embedded test server for integration testing Magnesium applications.
 *
 * <p>Usage with try-with-resources:
 * <pre>{@code
 * try (TestServer server = TestServer.run(new MyApplication())) {
 *     HttpResponse<String> response = server.client()
 *         .send(HttpRequest.newBuilder(server.uri("/health"))
 *             .build(),
 *             HttpResponse.BodyHandlers.ofString());
 *
 *     assertEquals(200, response.statusCode());
 *     assertEquals("healthy", response.body());
 * }
 * }</pre>
 *
 * <p>With custom configuration:
 * <pre>{@code
 * try (TestServer server = TestServer.run(new MyApplication(), runtime -> {
 *     runtime.timeout(Duration.ofSeconds(5)); // Faster timeouts for tests
 * })) {
 *     // ... test code
 * }
 * }</pre>
 */
public class TestServer implements AutoCloseable {

    private final Application application;
    private final MagnesiumRuntime runtime;
    private final MagnesiumTransport transport;
    private final int port;
    private final HttpClient httpClient;
    private final CountDownLatch shutdownLatch;
    private volatile boolean running = false;

    /**
     * Creates and starts a test server with the given application.
     *
     * @param application the application to test
     * @param port 0 for any available port, or specific port number
     */
    public TestServer(Application application, int port) {
        this(application, port, runtime -> {});
    }

    /**
     * Creates and starts a test server with custom runtime configuration.
     *
     * @param application the application to test
     * @param port 0 for any available port, or specific port number
     * @param configurer additional runtime configuration for testing
     */
    public TestServer(Application application, int port, Consumer<MagnesiumRuntime> configurer) {
        this.application = application;
        this.port = port;
        this.shutdownLatch = new CountDownLatch(1);

        this.runtime = new MagnesiumRuntime(application);
        application.configure(runtime);
        configurer.accept(runtime);
        runtime.freeze();

        // Load transport - use a lightweight one for tests if available
        this.transport = loadTestTransport();

        // Create HTTP client with reasonable defaults for tests
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        start();
    }

    /**
     * Factory method for convenient creation.
     */
    public static TestServer run(Application application) {
        return new TestServer(application, 0);
    }

    /**
     * Factory method with specific port.
     */
    public static TestServer run(Application application, int port) {
        return new TestServer(application, port);
    }

    /**
     * Factory method with custom configuration.
     */
    public static TestServer run(Application application, Consumer<MagnesiumRuntime> configurer) {
        return new TestServer(application, 0, configurer);
    }

    private void start() {
        if (running) {
            throw new IllegalStateException("Server already running");
        }

        // Start server in background thread
        Thread serverThread = new Thread(() -> {
            try {
                transport.bind(port, runtime, runtime.router().routes());
                running = true;

                // Wait for shutdown signal
                shutdownLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "test-server");
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for server to be ready (with timeout)
        waitForStartup();
    }

    private void waitForStartup() {
        int attempts = 0;
        int maxAttempts = 50; // 5 seconds total

        while (!running && attempts < maxAttempts) {
            try {
                Thread.sleep(100);
                attempts++;

                // Try to connect to verify it's up
                if (getPort() > 0) {
                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl() + "/"))
                            .timeout(Duration.ofMillis(100))
                            .build();
                        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                        // If we get here, server is up
                        break;
                    } catch (Exception e) {
                        // Expected while starting up
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for server", e);
            }
        }

        if (!running && attempts >= maxAttempts) {
            throw new RuntimeException("Server failed to start within timeout");
        }
    }

    private MagnesiumTransport loadTestTransport() {
        // Try to load a test-friendly transport
        // 1. First try to find any transport on classpath
        return java.util.ServiceLoader.load(MagnesiumTransport.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No MagnesiumTransport found on classpath. " +
                "Add a test transport dependency (e.g., magnesium-transport-httpserver)."));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HTTP Client Methods
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns the HTTP client configured for this server.
     */
    public HttpClient client() {
        return httpClient;
    }

    /**
     * Returns the base URL for the server (e.g., "http://localhost:8080").
     */
    public String baseUrl() {
        return "http://localhost:" + getPort();
    }

    /**
     * Returns a URI for the given path.
     */
    public URI uri(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(baseUrl() + normalizedPath);
    }

    /**
     * Convenience method for GET requests.
     */
    public HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path))
            .GET()
            .build());
    }

    /**
     * Convenience method for POST requests with string body.
     */
    public HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json")
            .build());
    }

    /**
     * Convenience method for PUT requests.
     */
    public HttpResponse<String> put(String path, String body) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path))
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json")
            .build());
    }

    /**
     * Convenience method for DELETE requests.
     */
    public HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path))
            .DELETE()
            .build());
    }

    /**
     * Sends a request and returns the response as a string.
     */
    public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends a request asynchronously.
     */
    public CompletableFuture<HttpResponse<String>> sendAsync(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Returns the actual port the server is listening on.
     * If port 0 was requested, this returns the dynamically assigned port.
     */
    public int getPort() {
        return transport.getPort();
    }

    /**
     * Returns true if the server is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns the runtime for direct inspection.
     */
    public MagnesiumRuntime runtime() {
        return runtime;
    }

    /**
     * Returns the application for inspection.
     */
    public Application application() {
        return application;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Shuts down the test server.
     */
    @Override
    public void close() {
        if (!running) {
            return;
        }

        running = false;
        shutdownLatch.countDown();

        // Give the server time to shut down
        try {
            if (!shutdownLatch.await(5, TimeUnit.SECONDS)) {
                System.err.println("Warning: Test server did not shut down cleanly");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Call application stop hook
        try {
            application.stop(runtime);
        } catch (Exception e) {
            System.err.println("Error during application stop: " + e.getMessage());
        }
    }

    /**
     * Waits for the server to handle all pending requests.
     */
    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }
}
