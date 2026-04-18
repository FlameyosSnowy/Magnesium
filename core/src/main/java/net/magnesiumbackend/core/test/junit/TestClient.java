package net.magnesiumbackend.core.test.junit;

import net.magnesiumbackend.core.test.TestServer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Fluent HTTP client for testing Magnesium endpoints.
 *
 * <p>Provides a convenient API for making requests and assertions:
 * <pre>{@code
 * @Test
 * void testApi(TestClient client) {
 *     client.get("/users/1")
 *         .expectStatus(200)
 *         .expectJsonPath("$.name", "Alice");
 *
 *     client.post("/users", "{\"name\":\"Bob\"}")
 *         .expectStatus(201)
 *         .expectHeader("Location", containsString("/users/"));
 * }
 * }</pre>
 */
public class TestClient {

    private final TestServer server;
    private final HttpClient httpClient;
    private final Map<String, String> defaultHeaders = new HashMap<>();

    public TestClient(TestServer server) {
        this.server = server;
        this.httpClient = server.client();
    }

    /**
     * Sets a default header for all subsequent requests.
     */
    public TestClient withHeader(String name, String value) {
        defaultHeaders.put(name, value);
        return this;
    }

    /**
     * Sets the Authorization header with a Bearer token.
     */
    public TestClient withAuth(String token) {
        return withHeader("Authorization", "Bearer " + token);
    }

    /**
     * Sets the Content-Type header.
     */
    public TestClient withContentType(String contentType) {
        return withHeader("Content-Type", contentType);
    }

    /**
     * Clears all default headers.
     */
    public TestClient clearHeaders() {
        defaultHeaders.clear();
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HTTP Methods
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Performs a GET request.
     */
    public TestResponse get(String path) {
        return execute(buildRequest(path, builder -> builder.GET()));
    }

    /**
     * Performs a POST request with a string body.
     */
    public TestResponse post(String path, String body) {
        return execute(buildRequest(path, builder ->
            builder.POST(HttpRequest.BodyPublishers.ofString(body))));
    }

    /**
     * Performs a POST request with JSON body.
     */
    public TestResponse postJson(String path, String json) {
        return post(path, json)
            .expectContentType("application/json");
    }

    /**
     * Performs a POST request with an object that will be serialized to JSON.
     */
    public TestResponse postObject(String path, Object body) {
        // Would use JSON serialization in real implementation
        throw new UnsupportedOperationException("JSON serialization not yet implemented");
    }

    /**
     * Performs a PUT request.
     */
    public TestResponse put(String path, String body) {
        return execute(buildRequest(path, builder ->
            builder.PUT(HttpRequest.BodyPublishers.ofString(body))));
    }

    /**
     * Performs a PUT request with JSON body.
     */
    public TestResponse putJson(String path, String json) {
        return withContentType("application/json").put(path, json);
    }

    /**
     * Performs a PATCH request.
     */
    public TestResponse patch(String path, String body) {
        return execute(buildRequest(path, builder ->
            builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body))));
    }

    /**
     * Performs a DELETE request.
     */
    public TestResponse delete(String path) {
        return execute(buildRequest(path, builder -> builder.DELETE()));
    }

    /**
     * Performs a HEAD request.
     */
    public TestResponse head(String path) {
        return execute(buildRequest(path, builder -> builder.method("HEAD",
            HttpRequest.BodyPublishers.noBody())));
    }

    /**
     * Performs an OPTIONS request.
     */
    public TestResponse options(String path) {
        return execute(buildRequest(path, builder -> builder.method("OPTIONS",
            HttpRequest.BodyPublishers.noBody())));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Request Building
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Starts building a custom request.
     */
    public RequestBuilder request(String path) {
        return new RequestBuilder(this, server.uri(path));
    }

    private HttpRequest buildRequest(String path,
                                     java.util.function.Consumer<HttpRequest.Builder> methodConfigurer) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(server.uri(path))
            .timeout(Duration.ofSeconds(10));

        // Apply default headers
        defaultHeaders.forEach(builder::header);

        // Apply method-specific configuration
        methodConfigurer.accept(builder);

        return builder.build();
    }

    TestResponse execute(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
            return new TestResponse(response, this);
        } catch (IOException e) {
            throw new TestClientException("Request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TestClientException("Request interrupted", e);
        }
    }

    CompletableFuture<TestResponse> executeAsync(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> new TestResponse(response, this));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Inner Classes
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builder for custom HTTP requests.
     */
    public static class RequestBuilder {
        private final TestClient client;
        private final HttpRequest.Builder builder;
        private final Map<String, String> headers = new HashMap<>();

        RequestBuilder(TestClient client, URI uri) {
            this.client = client;
            this.builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10));
        }

        public RequestBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public RequestBuilder auth(String token) {
            return header("Authorization", "Bearer " + token);
        }

        public RequestBuilder contentType(String type) {
            return header("Content-Type", type);
        }

        public TestResponse get() {
            return execute(builder.GET());
        }

        public TestResponse post(String body) {
            return execute(builder.POST(HttpRequest.BodyPublishers.ofString(body)));
        }

        public TestResponse post() {
            return execute(builder.POST(HttpRequest.BodyPublishers.noBody()));
        }

        public TestResponse put(String body) {
            return execute(builder.PUT(HttpRequest.BodyPublishers.ofString(body)));
        }

        public TestResponse delete() {
            return execute(builder.DELETE());
        }

        public TestResponse patch(String body) {
            return execute(builder.method("PATCH",
                HttpRequest.BodyPublishers.ofString(body)));
        }

        private TestResponse execute(HttpRequest.Builder finalBuilder) {
            headers.forEach((_, _) ->
                finalBuilder.header(headers.keySet().iterator().next(),
                    headers.get(headers.keySet().iterator().next())));
            return client.execute(finalBuilder.build());
        }
    }

    /**
     * Exception thrown by the test client.
     */
    public static class TestClientException extends RuntimeException {
        public TestClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
