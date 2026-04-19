import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.http.response.Response;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.test.TestServer;
import net.magnesiumbackend.core.test.junit.MagnesiumExtension;
import net.magnesiumbackend.core.test.junit.MagnesiumTest;
import net.magnesiumbackend.core.test.junit.TestClient;
import net.magnesiumbackend.core.test.junit.TestResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Example test class demonstrating Magnesium testing capabilities.
 *
 * <p>This shows multiple approaches:
 * 1. Full integration tests with @MagnesiumTest
 * 2. Direct assertions on TestResponse
 * 3. Using TestClient for fluent API
 */
class UserApiTest {

    public static class TestApplication extends Application {
        private final UserService userService = new UserService();

        @Override
        protected void configure(MagnesiumRuntime runtime) {
            runtime.services(s -> s
                .register(UserService.class, ctx -> userService)
            );

            runtime.router()
                .get("/health", ctx -> Response.ok(Map.of("status", "up")))

                .get("/users/:id", ctx -> {
                    User user = userService.findById(ctx.pathParam("id"));
                    if (user == null) {
                        return Response.status(404).body(Map.of("error", "User not found"));
                    }
                    return Response.ok(user);
                })

                .post("/users", ctx -> {
                    // Parse request (simplified)
                    String name = "New User"; // Would parse from body
                    User user = userService.create(name, "new@example.com");
                    return Response.status(201).body(user);
                })

                .delete("/users/:id", ctx -> {
                    userService.delete(ctx.pathParam("id"));
                    return Response.noContent();
                });
        }
    }

    public static class UserService {
        private final Map<String, User> users = new ConcurrentHashMap<>();
        private int idCounter = 0;

        public User create(String name, String email) {
            String id = String.valueOf(++idCounter);
            User user = new User(id, name, email);
            users.put(id, user);
            return user;
        }

        public User findById(String id) {
            return users.get(id);
        }

        public void delete(String id) {
            users.remove(id);
        }
    }

    public static class User {
        public String id;
        public String name;
        public String email;

        public User(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }

    @ExtendWith(MagnesiumExtension.class)
    @MagnesiumTest(TestApplication.class)
    static class IntegrationTests {

        @Test
        void healthEndpointReturnsOk(TestClient client) {
            client.get("/health")
                .expectOk()
                .expectContentTypeContains("application/json")
                .expectBodyContains("\"status\":\"up\"");
        }

        @Test
        void createAndRetrieveUser(TestClient client) {
            // Create a user
            TestResponse createResponse = client.postJson("/users", "{\"name\":\"Alice\"}");
            createResponse.expectCreated();

            // Extract ID from response (simplified)
            String body = createResponse.body();
            assertTrue(body.contains("\"id\":\"1\""));

            // Retrieve the user
            client.get("/users/1")
                .expectOk()
                .expectBodyContains("\"name\":\"Alice\"");
        }

        @Test
        void getNonExistentUserReturns404(TestClient client) {
            client.get("/users/999")
                .expectNotFound()
                .expectBodyContains("User not found");
        }

        @Test
        void deleteUser(TestClient client) {
            // Setup: create a user
            client.post("/users", "{\"name\":\"ToDelete\"}").expectCreated();

            // Delete it
            client.delete("/users/1").expectNoContent();

            // Verify it's gone
            client.get("/users/1").expectNotFound();
        }

        @Test
        void multipleRequestsShareState(TestClient client) {
            // The TestServer maintains the same application instance
            // so state persists across requests
            client.post("/users", "{\"name\":\"Bob\"}").expectCreated();
            client.post("/users", "{\"name\":\"Charlie\"}").expectCreated();

            // Users should have sequential IDs
            client.get("/users/1").expectOk();
            client.get("/users/2").expectOk();
        }
    }

    @Test
    void manualServerLifecycle() throws Exception {
        // For fine-grained control, manage the server yourself
        try (TestServer server = TestServer.run(new TestApplication())) {
            TestClient client = new TestClient(server);

            // Test code here
            client.get("/health").expectOk();

            // Server automatically shuts down at end of try block
        }
    }

    @Test
    void withCustomConfiguration() throws Exception {
        // Override runtime configuration for specific test needs
        try (TestServer server = TestServer.run(
            new TestApplication(),
            runtime -> runtime.requestTimeout(java.time.Duration.ofMillis(100))
        )) {
            TestClient client = new TestClient(server);

            client.get("/health").expectOk();
        }
    }

    @Test
    void usingRawHttpClient() throws Exception {
        try (TestServer server = TestServer.run(new TestApplication())) {
            java.net.http.HttpClient client = server.client();

            java.net.http.HttpResponse<String> response = client.send(
                java.net.http.HttpRequest.newBuilder(server.uri("/health"))
                    .GET()
                    .build(),
                java.net.http.HttpResponse.BodyHandlers.ofString()
            );

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("up"));
        }
    }

    @Test
    void asyncRequest() throws Exception {
        try (TestServer server = TestServer.run(new TestApplication())) {
            TestClient client = new TestClient(server);

            // Async requests return CompletableFuture
            java.util.concurrent.CompletableFuture<TestResponse> future =
                client.request("/health")
                    .getAsync();

            TestResponse response = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
            response.expectOk();
        }
    }

    @Test
    void testErrorResponse() throws Exception {
        try (TestServer server = TestServer.run(new TestApplication())) {
            TestClient client = new TestClient(server);

            // Expect a 404 with specific error message
            client.get("/nonexistent")
                .expectNotFound()
                .expectBodyContains("Not Found");
        }
    }

    @Test
    void testCustomHeaders() throws Exception {
        try (TestServer server = TestServer.run(new TestApplication())) {
            TestClient client = new TestClient(server)
                .withHeader("X-Custom-Header", "test-value")
                .withAuth("my-token-123");

            // All subsequent requests will include these headers
            client.get("/health").expectOk();

            // Clear and set different headers
            client.clearHeaders()
                .withHeader("X-Other", "value")
                .get("/health")
                .expectOk();
        }
    }
}
