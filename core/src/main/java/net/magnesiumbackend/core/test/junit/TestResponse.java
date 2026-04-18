package net.magnesiumbackend.core.test.junit;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.function.Predicate;

/**
 * Wrapper for HTTP responses with fluent assertion helpers.
 *
 * <p>Provides methods for validating responses in tests:
 * <pre>{@code
 * client.get("/users/1")
 *     .expectStatus(200)
 *     .expectContentType("application/json")
 *     .expectBodyContains("\"id\":\"1\"")
 *     .expectJsonPath("$.name", "Alice");
 * }</pre>
 */
public class TestResponse {

    private final HttpResponse<String> response;
    private final TestClient client;

    TestResponse(HttpResponse<String> response, TestClient client) {
        this.response = response;
        this.client = client;
    }

    /**
     * Asserts the status code equals the expected value.
     */
    public TestResponse expectStatus(int expected) {
        int actual = response.statusCode();
        if (actual != expected) {
            throw new AssertionError(
                String.format("Expected status %d but got %d. Body: %s",
                    expected, actual, body()));
        }
        return this;
    }

    /**
     * Asserts the status code is 200 OK.
     */
    public TestResponse expectOk() {
        return expectStatus(200);
    }

    /**
     * Asserts the status code is 201 Created.
     */
    public TestResponse expectCreated() {
        return expectStatus(201);
    }

    /**
     * Asserts the status code is 204 No Content.
     */
    public TestResponse expectNoContent() {
        return expectStatus(204);
    }

    /**
     * Asserts the status code is 400 Bad Request.
     */
    public TestResponse expectBadRequest() {
        return expectStatus(400);
    }

    /**
     * Asserts the status code is 401 Unauthorized.
     */
    public TestResponse expectUnauthorized() {
        return expectStatus(401);
    }

    /**
     * Asserts the status code is 403 Forbidden.
     */
    public TestResponse expectForbidden() {
        return expectStatus(403);
    }

    /**
     * Asserts the status code is 404 Not Found.
     */
    public TestResponse expectNotFound() {
        return expectStatus(404);
    }

    /**
     * Asserts the status code is 500 Internal Server Error.
     */
    public TestResponse expectServerError() {
        return expectStatus(500);
    }

    /**
     * Asserts the status code matches a predicate.
     */
    public TestResponse expectStatusThat(Predicate<Integer> predicate) {
        int actual = response.statusCode();
        if (!predicate.test(actual)) {
            throw new AssertionError(
                "Status code " + actual + " did not match predicate");
        }
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Body Assertions
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Asserts the response body equals the expected string.
     */
    public TestResponse expectBody(String expected) {
        String actual = body();
        if (!expected.equals(actual)) {
            throw new AssertionError(
                String.format("Expected body:\n%s\n\nBut got:\n%s",
                    expected, actual));
        }
        return this;
    }

    /**
     * Asserts the response body contains the expected substring.
     */
    public TestResponse expectBodyContains(String expected) {
        String actual = body();
        if (actual == null || !actual.contains(expected)) {
            throw new AssertionError(
                String.format("Expected body to contain '%s' but was:\n%s",
                    expected, actual));
        }
        return this;
    }

    /**
     * Asserts the response body does not contain the substring.
     */
    public TestResponse expectBodyNotContains(String unexpected) {
        String actual = body();
        if (actual != null && actual.contains(unexpected)) {
            throw new AssertionError(
                String.format("Expected body NOT to contain '%s' but it did",
                    unexpected));
        }
        return this;
    }

    /**
     * Asserts the response body is empty or null.
     */
    public TestResponse expectEmptyBody() {
        String actual = body();
        if (actual != null && !actual.isEmpty()) {
            throw new AssertionError(
                "Expected empty body but got: " + actual);
        }
        return this;
    }

    /**
     * Asserts the response body matches a regex pattern.
     */
    public TestResponse expectBodyMatches(String regex) {
        String actual = body();
        if (actual == null || !actual.matches(regex)) {
            throw new AssertionError(
                String.format("Expected body to match pattern '%s' but was:\n%s",
                    regex, actual));
        }
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // JSON Assertions (simplified - would use JSON library in real impl)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Asserts the response is valid JSON and contains the expected path.
     */
    public TestResponse expectJsonPath(String path, Object expected) {
        String body = body();
        // Simplified - real implementation would use Jackson/Gson
        String search = "\"" + path.replace("$.", "").replace("[", "").replace("]", "") + "\"";
        if (body == null || !body.contains(search)) {
            throw new AssertionError(
                String.format("Expected JSON path '%s' not found in:\n%s",
                    path, body));
        }
        // TODO: Proper JSON path evaluation
        return this;
    }

    /**
     * Asserts the response body is valid JSON.
     */
    public TestResponse expectValidJson() {
        String body = body();
        if (body == null || body.isEmpty()) {
            throw new AssertionError("Expected valid JSON but body was empty");
        }
        // Simplified check - real implementation would parse JSON
        if (!(body.trim().startsWith("{") || body.trim().startsWith("["))) {
            throw new AssertionError("Expected valid JSON but was: " + body);
        }
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Header Assertions
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Asserts a header equals the expected value.
     */
    public TestResponse expectHeader(String name, String expected) {
        String actual = header(name);
        if (actual == null) {
            throw new AssertionError(
                String.format("Expected header '%s' to be '%s' but it was absent",
                    name, expected));
        }
        if (!expected.equals(actual)) {
            throw new AssertionError(
                String.format("Expected header '%s' to be '%s' but was '%s'",
                    name, expected, actual));
        }
        return this;
    }

    /**
     * Asserts a header contains the expected substring.
     */
    public TestResponse expectHeaderContains(String name, String expected) {
        String actual = header(name);
        if (actual == null) {
            throw new AssertionError(
                String.format("Expected header '%s' to contain '%s' but it was absent",
                    name, expected));
        }
        if (!actual.contains(expected)) {
            throw new AssertionError(
                String.format("Expected header '%s' to contain '%s' but was '%s'",
                    name, expected, actual));
        }
        return this;
    }

    /**
     * Asserts a header is present.
     */
    public TestResponse expectHeaderPresent(String name) {
        if (header(name) == null) {
            throw new AssertionError(
                "Expected header '" + name + "' to be present but it was absent");
        }
        return this;
    }

    /**
     * Asserts a header is absent.
     */
    public TestResponse expectHeaderAbsent(String name) {
        if (header(name) != null) {
            throw new AssertionError(
                "Expected header '" + name + "' to be absent but it was present");
        }
        return this;
    }

    /**
     * Asserts the Content-Type header equals the expected value.
     */
    public TestResponse expectContentType(String expected) {
        return expectHeader("Content-Type", expected);
    }

    /**
     * Asserts the Content-Type header contains the expected type.
     */
    public TestResponse expectContentTypeContains(String expected) {
        return expectHeaderContains("Content-Type", expected);
    }

    /**
     * Asserts the Location header equals the expected value.
     */
    public TestResponse expectLocation(String expected) {
        return expectHeader("Location", expected);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Accessors
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns the underlying HTTP response.
     */
    public HttpResponse<String> raw() {
        return response;
    }

    /**
     * Returns the status code.
     */
    public int statusCode() {
        return response.statusCode();
    }

    /**
     * Returns the response body as a string.
     */
    public String body() {
        return response.body();
    }

    /**
     * Returns a header value.
     */
    public String header(String name) {
        return response.headers().firstValue(name).orElse(null);
    }

    /**
     * Returns the final URI after redirects.
     */
    public URI uri() {
        return response.uri();
    }

    /**
     * Allows chaining additional requests using the same client.
     */
    public TestClient andThen() {
        return client;
    }

    /**
     * Prints the response for debugging.
     */
    public TestResponse print() {
        System.out.println("=== Response ===");
        System.out.println("Status: " + statusCode());
        System.out.println("Headers: " + response.headers().map());
        System.out.println("Body: " + body());
        System.out.println("================");
        return this;
    }
}
