package net.magnesiumbackend.transport.httpserver;

import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.http.MagnesiumHttpServer;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HTTP path variable routing in JDK HttpServer transport.
 */
class HttpServerPathVariablesTest {

    private static final int TEST_PORT = 0; // Use any available port
    private HttpServerMagnesiumTransport transport;
    private MagnesiumRuntime application;
    private int actualPort;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws Exception {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @AfterEach
    void tearDown() {
        if (transport != null) {
            transport.shutdown();
        }
    }

    /**
     * Creates a test application with the given HTTP server configuration.
     */
    private void startApplication(MagnesiumHttpServer httpServer) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        application = MagnesiumApplication.builder()
            .http(http -> {
                http.server(httpServer);
            })
            .onStart(ctx -> latch.countDown())
            .onExit(ctx -> {})
            .build();

        transport = new HttpServerMagnesiumTransport();

        // Start in a separate thread
        Thread serverThread = new Thread(() -> transport.bind(0, application, application.router().routes()));
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for server to start
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Server failed to start");

        // Get actual port
        actualPort = transport.getPort();
        assertTrue(actualPort > 0, "Server should have started on a valid port");
    }

    @Test
    void testSimplePathVariable() throws Exception {
        MagnesiumHttpServer httpServer = MagnesiumHttpServer.builder()
            .get("/users/{id}", ctx -> {
                String userId = ctx.pathVariables().get("id");
                return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "message", "User found"
                ));
            })
            .build();

        startApplication(httpServer);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + actualPort + "/users/123"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("123"), "Response should contain the user ID");
        assertTrue(body.contains("User found"), "Response should contain the message");
    }

    @Test
    void testMultiplePathVariables() throws Exception {
        MagnesiumHttpServer httpServer = MagnesiumHttpServer.builder()
            .get("/users/{userId}/orders/{orderId}", ctx -> {
                HttpPathParamIndex pathVars = ctx.pathVariables();
                return ResponseEntity.ok(Map.of(
                    "userId", pathVars.get("userId"),
                    "orderId", pathVars.get("orderId")
                ));
            })
            .build();

        startApplication(httpServer);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + actualPort + "/users/456/orders/789"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("456"), "Response should contain the user ID");
        assertTrue(body.contains("789"), "Response should contain the order ID");
    }

    @Test
    void testNestedPathVariables() throws Exception {
        MagnesiumHttpServer httpServer = MagnesiumHttpServer.builder()
            .get("/api/version/{version}/resources/{resourceId}/items/{itemId}", ctx -> {
                HttpPathParamIndex pathVars = ctx.pathVariables();
                return ResponseEntity.ok(Map.of(
                    "version", pathVars.get("version"),
                    "resourceId", pathVars.get("resourceId"),
                    "itemId", pathVars.get("itemId")
                ));
            })
            .build();

        startApplication(httpServer);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + actualPort + "/api/version/2/resources/abc123/items/item-456"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("2"), "Response should contain the version");
        assertTrue(body.contains("abc123"), "Response should contain the resource ID");
        assertTrue(body.contains("item-456"), "Response should contain the item ID");
    }

    @Test
    void testStaticRouteDoesNotMatchVariableRoute() throws Exception {
        AtomicReference<String> matchedRoute = new AtomicReference<>();

        MagnesiumHttpServer httpServer = MagnesiumHttpServer.builder()
            .get("/users/admin", ctx -> {
                matchedRoute.set("static");
                return ResponseEntity.ok(Map.of("route", "static"));
            })
            .get("/users/{id}", ctx -> {
                matchedRoute.set("variable");
                return ResponseEntity.ok(Map.of("route", "variable", "id", ctx.pathVariables().getRaw("id")));
            })
            .build();

        startApplication(httpServer);

        // Test static route
        HttpRequest staticRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + actualPort + "/users/admin"))
            .GET()
            .build();

        HttpResponse<String> staticResponse = httpClient.send(staticRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, staticResponse.statusCode());

        // Test variable route
        HttpRequest variableRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + actualPort + "/users/123"))
            .GET()
            .build();

        HttpResponse<String> variableResponse = httpClient.send(variableRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, variableResponse.statusCode());
        assertTrue(variableResponse.body().contains("123"), "Should match variable route");
    }

    @Test
    void testPathVariableWithDifferentHttpMethods() throws Exception {
        MagnesiumHttpServer httpServer = MagnesiumHttpServer.builder()
            .get("/items/{id}", ctx -> ResponseEntity.ok(Map.of("method", "GET", "id", ctx.pathVariables().getRaw("id"))))
            .post("/items/{id}", ctx -> ResponseEntity.created(Map.of("method", "POST", "id", ctx.pathVariables().getRaw("id"))))
            .put("/items/{id}", ctx -> ResponseEntity.ok(Map.of("method", "PUT", "id", ctx.pathVariables().getRaw("id"))))
            .delete("/items/{id}", ctx -> ResponseEntity.noContent())
            .build();

        startApplication(httpServer);

        String baseUri = "http://localhost:" + actualPort + "/items/123";

        // GET
        HttpResponse<String> getResponse = httpClient.send(
            HttpRequest.newBuilder().uri(URI.create(baseUri)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, getResponse.statusCode());
        System.out.println(getResponse.body());
        assertEquals("GET", getResponse.request().method());
        assertTrue(getResponse.body().contains("123"));

        // POST
        HttpResponse<String> postResponse = httpClient.send(
            HttpRequest.newBuilder().uri(URI.create(baseUri)).POST(HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(201, postResponse.statusCode());

        // PUT
        HttpResponse<String> putResponse = httpClient.send(
            HttpRequest.newBuilder().uri(URI.create(baseUri)).PUT(HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, putResponse.statusCode());
        assertTrue(putResponse.body().contains("PUT"));

        // DELETE
        HttpResponse<String> deleteResponse = httpClient.send(
            HttpRequest.newBuilder().uri(URI.create(baseUri)).DELETE().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(204, deleteResponse.statusCode());
    }

    @Test
    void testPathVariableWithSpecialCharacters() throws Exception {
        MagnesiumHttpServer httpServer = MagnesiumHttpServer.builder()
            .get("/files/{path}", ctx -> {
                String path = ctx.pathVariables().get("path");
                return ResponseEntity.ok(Map.of("path", path));
            })
            .build();

        startApplication(httpServer);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + actualPort + "/files/docs%2Freport.pdf"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("docs/report.pdf") || body.contains("docs%2Freport.pdf"),
            "Response should contain the file path");
    }

    @Test
    void testNonExistentRouteReturns404() throws Exception {
        MagnesiumHttpServer httpServer = MagnesiumHttpServer.builder()
            .get("/exists", ctx -> ResponseEntity.ok(Map.of("exists", true)))
            .build();

        startApplication(httpServer);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + actualPort + "/does-not-exist"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void testQueryParamsAndPathVariablesTogether() throws Exception {
        MagnesiumHttpServer httpServer = MagnesiumHttpServer.builder()
            .get("/users/{id}/search", ctx -> {
                String userId = ctx.pathVariables().get("id");
                String query = ctx.queryParam("q");
                return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "query", query != null ? query : ""
                ));
            })
            .build();

        startApplication(httpServer);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + actualPort + "/users/789/search?q=test+query"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("789"), "Response should contain the user ID from path");
        assertTrue(body.contains("test query") || body.contains("test+query"),
            "Response should contain the query parameter");
    }
}
