package net.magnesiumbackend.core.test;

import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.headers.HttpQueryParamIndex;
import net.magnesiumbackend.core.http.DefaultRequest;
import net.magnesiumbackend.core.http.response.HttpVersion;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.RouteTree;
import net.magnesiumbackend.core.services.ServiceRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Test harness for Magnesium applications.
 *
 * <p>Provides utilities for testing HTTP endpoints without starting a full server:
 * <pre>{@code
 * @Test
 * void testUserEndpoint() {
 *     harness.route(GET, "/users/:id", ctx -> Response.ok(new User("1", "Alice")));
 *
 *     TestResponse response = harness.get("/users/1");
 *
 *     assertEquals(200, response.statusCode());
 *     assertEquals("Alice", response.json().get("name").asText());
 * }
 * }</pre>
 *
 * <p>For integration tests with a real server:
 * <pre>{@code
 * @Test
 * void integrationTest() {
 *     try (TestServer server = TestServer.run(new MyApplication())) {
 *         HttpResponse<String> response = server.client()
 *             .send(HttpRequest.newBuilder()
 *                 .uri(server.uri("/health"))
 *                 .build(),
 *                 HttpResponse.BodyHandlers.ofString());
 *
 *         assertEquals(200, response.statusCode());
 *     }
 * }
 * }</pre>
 */
public class MagnesiumTestHarness {

    private final MagnesiumRuntime runtime;
    private final HttpRouteRegistry routeRegistry;
    private final Map<String, Object> attributes = new HashMap<>();

    public MagnesiumTestHarness() {
        this.runtime = new TestRuntime(null);
        this.routeRegistry = runtime.router().routes();
    }

    /**
     * Registers a GET route for testing.
     */
    public MagnesiumTestHarness get(String path, TestHandler handler) {
        routeRegistry.register(net.magnesiumbackend.core.http.response.HttpMethod.GET,
            net.magnesiumbackend.core.route.RoutePathTemplate.compile(path.getBytes()),
            handler.toRouteHandler(),
            java.util.List.of());
        return this;
    }

    /**
     * Registers a POST route for testing.
     */
    public MagnesiumTestHarness post(String path, TestHandler handler) {
        routeRegistry.register(net.magnesiumbackend.core.http.response.HttpMethod.POST,
            net.magnesiumbackend.core.route.RoutePathTemplate.compile(path.getBytes()),
            handler.toRouteHandler(),
            java.util.List.of());
        return this;
    }

    /**
     * Registers a PUT route for testing.
     */
    public MagnesiumTestHarness put(String path, TestHandler handler) {
        routeRegistry.register(net.magnesiumbackend.core.http.response.HttpMethod.PUT,
            net.magnesiumbackend.core.route.RoutePathTemplate.compile(path.getBytes()),
            handler.toRouteHandler(),
            java.util.List.of());
        return this;
    }

    /**
     * Registers a DELETE route for testing.
     */
    public MagnesiumTestHarness delete(String path, TestHandler handler) {
        routeRegistry.register(net.magnesiumbackend.core.http.response.HttpMethod.DELETE,
            net.magnesiumbackend.core.route.RoutePathTemplate.compile(path.getBytes()),
            handler.toRouteHandler(),
            java.util.List.of());
        return this;
    }

    /**
     * Adds a global filter to the test harness.
     */
    public MagnesiumTestHarness filter(HttpFilter filter) {
        runtime.router().filter(filter);
        return this;
    }

    /**
     * Executes a GET request against the registered routes.
     */
    public TestResponse get(String path) {
        return execute(net.magnesiumbackend.core.http.response.HttpMethod.GET, path, null, Map.of());
    }

    /**
     * Executes a GET request with headers.
     */
    public TestResponse get(String path, Map<String, String> headers) {
        return execute(net.magnesiumbackend.core.http.response.HttpMethod.GET, path, null, headers);
    }

    /**
     * Executes a POST request with a body.
     */
    public TestResponse post(String path, String body) {
        return post(path, body, Map.of());
    }

    /**
     * Executes a POST request with a body and headers.
     */
    public TestResponse post(String path, String body, Map<String, String> headers) {
        return execute(net.magnesiumbackend.core.http.response.HttpMethod.POST, path,
            body != null ? body.getBytes() : null, headers);
    }

    /**
     * Executes a POST request with JSON body (sets Content-Type).
     */
    public TestResponse postJson(String path, String json) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return post(path, json, headers);
    }

    /**
     * Executes a PUT request with a body.
     */
    public TestResponse put(String path, String body) {
        return execute(net.magnesiumbackend.core.http.response.HttpMethod.PUT, path,
            body != null ? body.getBytes() : null, Map.of());
    }

    /**
     * Executes a DELETE request.
     */
    public TestResponse delete(String path) {
        return execute(net.magnesiumbackend.core.http.response.HttpMethod.DELETE, path, null, Map.of());
    }

    /**
     * Executes a request with full control.
     */
    public TestResponse execute(
        net.magnesiumbackend.core.http.response.HttpMethod method,
        String path,
        byte[] body,
        Map<String, String> headers
    ) {
        Map<String, List<String>> headerMap = new HashMap<>();
        headers.forEach((key, value) -> headerMap.put(key, List.of(value)));
        RouteTree.RouteMatch<RouteDefinition> match = routeRegistry.find(method, path);
        if (match == null) {
            throw new RuntimeException("No route found for " + method + " " + path);
        }

        RouteDefinition route = match.handler();
        DefaultRequest request = new DefaultRequest(
            path,
            body,
            HttpVersion.HTTP_1_1,
            method,
            HttpQueryParamIndex.empty(),
            HttpPathParamIndex.empty(),
            route,
            new HttpHeaderIndex(headerMap)
        );
        RequestContext requestContext = new RequestContext(request);
        ResponseEntity<?> execute = route.execute(requestContext, List.of(), new ExceptionHandlerRegistry());
        return new TestResponse(execute.statusCode(), null, execute.headers());
    }

    /**
     * Registers a service in the test runtime.
     */
    public <T> MagnesiumTestHarness registerService(Class<T> type, T instance) {
        // Services are typically registered during configure(),
        // but for tests we might want to mock them
        attributes.put(type.getName(), instance);
        return this;
    }

    /**
     * Gets the test runtime for direct manipulation.
     */
    public MagnesiumRuntime runtime() {
        return runtime;
    }

    /**
     * Gets the service registry.
     */
    public ServiceRegistry serviceRegistry() {
        return runtime.serviceRegistry();
    }

    /**
     * Starts a test server with the given application.
     *
     * @param application the application to test
     * @return a TestServer instance that can be used with try-with-resources
     */
    public static TestServer run(Application application) {
        return run(application, 0); // 0 = any available port
    }

    /**
     * Starts a test server on a specific port.
     */
    public static TestServer run(Application application, int port) {
        return new TestServer(application, port);
    }

    /**
     * Starts a test server with additional configuration.
     */
    public static TestServer run(Application application, Consumer<MagnesiumRuntime> configurer) {
        return new TestServer(application, 0, configurer);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Inner Classes
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Functional interface for test handlers.
     */
    @FunctionalInterface
    public interface TestHandler {
        ResponseEntity handle(TestRequestContext ctx);

        default net.magnesiumbackend.core.route.HttpRouteHandler toRouteHandler() {
            return ctx -> {
                TestRequestContext testCtx = new TestRequestContext(ctx);
                ResponseEntity response = handle(testCtx);
                return java.util.concurrent.CompletableFuture.completedFuture(response);
            };
        }
    }

    /**
     * Test request context with convenience methods.
     */
    public static class TestRequestContext {
        private final net.magnesiumbackend.core.route.RequestContext ctx;

        public TestRequestContext(net.magnesiumbackend.core.route.RequestContext ctx) {
            this.ctx = ctx;
        }

        public String pathParam(String name) {
            return ctx.pathVariables().get(name);
        }

        public String queryParam(String name) {
            return ctx.queryParam(name);
        }

        public String header(String name) {
            return ctx.header(name);
        }

        public byte[] body() {
            return ctx.request().bodyAsBytes();
        }

        public String bodyAsString() {
            return ctx.request().body();
        }

        public <T> T bodyAs(Class<T> type) {
            // Would use the message converter registry in real impl
            throw new UnsupportedOperationException("JSON parsing not yet implemented");
        }
    }

    /**
     * Test response with assertion helpers.
     */
    public static class TestResponse {
        private final int statusCode;
        private final byte[] body;
        private final HttpHeaderIndex headers;

        public TestResponse(int statusCode, byte[] body, HttpHeaderIndex headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }

        public int statusCode() {
            return statusCode;
        }

        public byte[] body() {
            return body;
        }

        public String bodyAsString() {
            return body != null ? new String(body, java.nio.charset.StandardCharsets.UTF_8) : null;
        }

        public String header(String name) {
            return headers.get(name.toLowerCase()).materialize();
        }

        // Assertion helpers
        public TestResponse assertStatus(int expected) {
            if (statusCode != expected) {
                throw new AssertionError(
                    "Expected status " + expected + " but got " + statusCode);
            }
            return this;
        }

        public TestResponse assertOk() {
            return assertStatus(200);
        }

        public TestResponse assertCreated() {
            return assertStatus(201);
        }

        public TestResponse assertNotFound() {
            return assertStatus(404);
        }

        public TestResponse assertError() {
            return assertStatus(500);
        }

        public TestResponse assertBodyContains(String expected) {
            String body = bodyAsString();
            if (body == null || !body.contains(expected)) {
                throw new AssertionError(
                    "Expected body to contain '" + expected + "' but was: " + body);
            }
            return this;
        }

        public TestResponse assertHeader(String name, String expected) {
            String actual = header(name);
            if (!expected.equals(actual)) {
                throw new AssertionError(
                    "Expected header '" + name + "' to be '" + expected +
                    "' but was '" + actual + "'");
            }
            return this;
        }
    }

    /**
     * Test runtime with test-specific overrides.
     */
    private static class TestRuntime extends MagnesiumRuntime {
        TestRuntime(Application app) {
            // Minimal setup for testing
            super(app);
        }
    }
}
