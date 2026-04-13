package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.HttpVersion;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.Slice;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an incoming HTTP request.
 *
 * <p>Request provides access to all aspects of the HTTP request including
 * the path, method, headers, query parameters, path variables, and body.
 * It is the primary input to route handlers.</p>
 *
 * <h3>Access Patterns</h3>
 * <pre>{@code
 * // Basic info
 * String path = request.path();
 * HttpMethod method = request.method();
 *
 * // Headers (efficient Slice-based)
 * Slice authHeader = request.header("Authorization");
 *
 * // Path variables from route
 * Map<String, String> vars = request.pathVariables();
 * String userId = vars.get("id");
 *
 * // Query parameters
 * String page = request.queryParam("page");
 *
 * // Cookies
 * Map<String, String> cookies = request.cookies();
 * }</pre>
 *
 * @see RequestContext
 * @see HttpMethod
 * @see HttpHeaderIndex
 */
public interface Request {

    /**
     * Returns the request path (excluding query string).
     *
     * @return the path (e.g., "/users/123")
     */
    String path();

    /**
     * Returns the route definition that matched this request.
     *
     * @return the matching route definition
     */
    RouteDefinition routeDefinition();

    /**
     * Returns the HTTP method.
     *
     * @return the method (GET, POST, etc.)
     */
    HttpMethod method();

    /**
     * Returns the request body as a String.
     *
     * @return the body content, or empty string if none
     */
    String body();

    /**
     * Returns the HTTP version.
     *
     * @return the version (HTTP_1_1, HTTP_2)
     */
    HttpVersion version();

    /**
     * Returns the path variables extracted from the route.
     *
     * @return map of variable name to value
     */
    Map<String, String> pathVariables();

    /**
     * Returns the parsed HTTP headers.
     *
     * @return the header index for efficient lookups
     */
    HttpHeaderIndex headerIndex();

    /**
     * Returns the query parameters.
     *
     * @return map of parameter name to value
     */
    Map<String, String> queryParams();

    /**
     * Returns a header value by name.
     *
     * @param name the header name (case-insensitive)
     * @return the header value as a Slice, or null if not present
     */
    @Nullable Slice header(String name);

    default Map<String, String> cookies() {
        Slice cookieHeader = header("Cookie");

        if (cookieHeader == null || cookieHeader.length() == 0) {
            return Collections.emptyMap();
        }

        int i = 0;
        int end = cookieHeader.length();

        Map<String, String> cookies = new HashMap<>();

        while (i < end) {

            // skip spaces + semicolons
            while (i < end) {
                char c = cookieHeader.charAt(i);
                if (c != ' ' && c != ';') break;
                i++;
            }

            if (i >= end) break;

            int keyStart = i;

            while (i < end) {
                char c = cookieHeader.charAt(i);
                if (c == '=' || c == ';') break;
                i++;
            }

            int keyEnd = i;

            if (i >= end || cookieHeader.charAt(i) != '=') {
                while (i < end && cookieHeader.charAt(i) != ';') i++;
                continue;
            }

            // trim key
            while (keyStart < keyEnd && cookieHeader.charAt(keyStart) == ' ') keyStart++;
            while (keyEnd > keyStart && cookieHeader.charAt(keyEnd - 1) == ' ') keyEnd--;

            i++; // '='

            int valueStart = i;

            while (i < end && cookieHeader.charAt(i) != ';') {
                i++;
            }

            int valueEnd = i;

            // trim value
            while (valueStart < valueEnd && cookieHeader.charAt(valueStart) == ' ') valueStart++;
            while (valueEnd > valueStart && cookieHeader.charAt(valueEnd - 1) == ' ') valueEnd--;

            if (keyEnd > keyStart) {

                Slice keySlice = cookieHeader.slice(keyStart, keyEnd);
                Slice valueSlice = cookieHeader.slice(valueStart, valueEnd);

                String key = keySlice.materialize();
                String value = valueSlice.materialize();

                if (!key.isEmpty()) {
                    cookies.put(key, value);
                }
            }
        }

        return Collections.unmodifiableMap(cookies);
    }

    default @Nullable String queryParam(String name) {
        return queryParams().get(name);
    }
}