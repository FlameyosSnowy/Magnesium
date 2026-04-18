# Magnesium Test Harness

Testing utilities for Magnesium applications.

## Quick Start

### 1. JUnit 5 Integration (Recommended)

```java
@ExtendWith(MagnesiumExtension.class)
@MagnesiumTest(MyApplication.class)
class MyIntegrationTest {

    @Test
    void testEndpoint(TestClient client) {
        client.get("/health")
            .expectStatus(200)
            .expectBodyContains("up");
    }
}
```

### 2. Manual TestServer Management

```java
@Test
void testWithManualServer() throws Exception {
    try (TestServer server = TestServer.run(new MyApplication())) {
        TestClient client = new TestClient(server);

        client.get("/api/users/1")
            .expectOk()
            .expectJsonPath("$.name", "Alice");
    }
}
```

### 3. Direct HTTP Client

```java
@Test
void testWithRawClient() throws Exception {
    try (TestServer server = TestServer.run(new MyApplication())) {
        HttpClient client = server.client();

        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder(server.uri("/health")).build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());
    }
}
```

## API Reference

### TestClient

Fluent HTTP client with request building:

```java
// Simple requests
client.get("/users/1");
client.post("/users", json);
client.put("/users/1", json);
client.delete("/users/1");

// With default headers
client.withHeader("X-Api-Key", "secret")
      .withAuth("token")
      .get("/protected");

// Custom request builder
client.request("/users")
    .header("X-Custom", "value")
    .auth("token")
    .post(json);
```

### TestResponse

Fluent assertions on responses:

```java
client.get("/users/1")
    // Status assertions
    .expectStatus(200)
    .expectOk()
    .expectNotFound()

    // Body assertions
    .expectBody("{\"id\":1}")
    .expectBodyContains("\"name\":\"Alice\"")
    .expectValidJson()
    .expectJsonPath("$.name", "Alice")

    // Header assertions
    .expectHeader("Content-Type", "application/json")
    .expectHeaderContains("Content-Type", "json")
    .expectLocation("/users/1")

    // Chain another request
    .andThen()
    .get("/users/2");
```

### @MagnesiumTest Options

```java
@MagnesiumTest(
    value = MyApplication.class,     // Required: Application class
    port = 9090,                      // Optional: fixed port (0 = random)
    callStart = true,                 // Optional: call start() lifecycle
    callStop = true                   // Optional: call stop() lifecycle
)
```

### TestServer Utilities

```java
// Server information
server.baseUrl();      // e.g., "http://localhost:8080"
server.uri("/path");   // URI for path
server.getPort();      // Actual port (if random)
server.runtime();      // Access the MagnesiumRuntime

// Direct HTTP requests
server.get("/health");
server.post("/users", json);
server.put("/users/1", json);
server.delete("/users/1");

// Send custom requests
server.send(HttpRequest.newBuilder(server.uri("/custom")).build());
```

## Examples

### Testing with Service Mocking

```java
@Test
void testWithMockedService() throws Exception {
    MyApplication app = new MyApplication() {
        @Override
        protected void configure(MagnesiumRuntime runtime) {
            super.configure(runtime);

            // Replace service with mock
            runtime.services(s -> s
                .register(UserService.class, ctx -> mockUserService)
            );
        }
    };

    try (TestServer server = TestServer.run(app)) {
        // Test against mocked service
    }
}
```

### Testing Error Scenarios

```java
@Test
void testErrorHandling(TestClient client) {
    client.get("/invalid")
        .expectBadRequest()
        .expectBodyContains("error");

    client.get("/not-found")
        .expectNotFound();

    client.get("/server-error")
        .expectServerError();
}
```

### Async Testing

```java
@Test
void testAsyncEndpoint() throws Exception {
    try (TestServer server = TestServer.run(new MyApplication())) {
        TestClient client = new TestClient(server);

        CompletableFuture<TestResponse> future =
            client.request("/async-endpoint").getAsync();

        TestResponse response = future.get(5, TimeUnit.SECONDS);
        response.expectOk();
    }
}
```

### Chaining Requests

```java
@Test
void testUserLifecycle(TestClient client) {
    // Create
    String userId = client.postJson("/users", userJson)
        .expectCreated()
        .andThen()  // Continue using same client
        .get("/users/1")
        .expectOk()
        .body();  // Extract ID from response

    // Update
    client.putJson("/users/1", updatedJson)
        .expectOk();

    // Delete
    client.delete("/users/1")
        .expectNoContent();

    // Verify deleted
    client.get("/users/1")
        .expectNotFound();
}
```

## Setup Requirements

Add the test harness to your project:

### Maven

```xml
<dependency>
    <groupId>net.magnesiumbackend</groupId>
    <artifactId>magnesium-core</artifactId>
    <version>${version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

### Gradle

```groovy
testImplementation 'net.magnesiumbackend:magnesium-core:${version}'
testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
```

### Required Test Transport

You need a transport implementation on the test classpath:

```xml
<!-- Lightweight option for tests -->
<dependency>
    <groupId>net.magnesiumbackend</groupId>
    <artifactId>magnesium-transport-httpserver</artifactId>
    <version>${version}</version>
    <scope>test</scope>
</dependency>
```

## Best Practices

1. **Use try-with-resources** for manual TestServer management to ensure cleanup
2. **Use random ports** (port = 0) to avoid conflicts in CI environments
3. **Keep tests focused** - one test per behavior, use @MagnesiumTest for multiple tests
4. **Use TestClient fluent API** for readable, chainable assertions
5. **Mock services** for unit-style integration tests
6. **Use custom configuration** for testing timeouts, error scenarios, etc.
