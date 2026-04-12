package net.magnesiumbackend.transport.httpserver;

import net.magnesiumbackend.core.annotations.Async;
import net.magnesiumbackend.core.annotations.GetMapping;
import net.magnesiumbackend.core.annotations.PathParam;
import net.magnesiumbackend.core.annotations.RestController;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.RequestContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class MyController {

    // 1. Just RequestContext (existing behavior)
    @GetMapping(path = "/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello, World!");
    }

    // 2. Async method with CompletableFuture
    @GetMapping(path = "/hello")
    public CompletableFuture<ResponseEntity<String>> helloAsync() {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok("Hello, World!"));
    }

    // 3. Single @PathParam
    @GetMapping(path = "/hello/{name}")
    public ResponseEntity<String> hello(@PathParam String name) {
        return ResponseEntity.ok("Hello, " + name + "!");
    }

    // 4. RequestContext with @PathParam
    @GetMapping(path = "/users/{userId}")
    public ResponseEntity<String> getUser(RequestContext ctx, @PathParam String userId) {
        return ResponseEntity.ok("User: " + userId);
    }

    // 5. RequestContext with multiple @PathParam
    @GetMapping(path = "/users/{userId}/orders/{orderId}")
    public ResponseEntity<String> getUserOrder(
            RequestContext ctx,
            @PathParam String userId,
            @PathParam String orderId) {
        return ResponseEntity.ok("User: " + userId + ", Order: " + orderId);
    }

    // 6. Multiple @PathParam without RequestContext
    @GetMapping(path = "/products/{categoryId}/{productId}")
    public ResponseEntity<String> getProduct(
            @PathParam String categoryId,
            @PathParam String productId) {
        return ResponseEntity.ok("Category: " + categoryId + ", Product: " + productId);
    }

    // 7. @PathParam with UUID type conversion
    @GetMapping(path = "/items/{itemId}")
    public ResponseEntity<String> getItem(@PathParam UUID itemId) {
        return ResponseEntity.ok("Item UUID: " + itemId);
    }

    // 8. @PathParam with int type conversion
    @GetMapping(path = "/posts/{postId}")
    public ResponseEntity<String> getPost(@PathParam int postId) {
        return ResponseEntity.ok("Post ID: " + postId);
    }

    // 9. @PathParam with long type conversion
    @GetMapping(path = "/records/{recordId}")
    public ResponseEntity<String> getRecord(@PathParam long recordId) {
        return ResponseEntity.ok("Record ID: " + recordId);
    }

    // 10. Mixed: RequestContext, @PathParam, and @PathParam with different types
    @GetMapping(path = "/api/v{version}/users/{userId}/posts/{postId}")
    public ResponseEntity<String> getApiVersionedUserPost(
            RequestContext ctx,
            @PathParam int version,
            @PathParam UUID userId,
            @PathParam int postId) {
        return ResponseEntity.ok("API v" + version + ", User: " + userId + ", Post: " + postId);
    }

    // 11. Return raw String without ResponseEntity wrapper (automatically wrapped)
    @GetMapping(path = "/simple/{name}")
    public String simpleHello(@PathParam String name) {
        return "Hello, " + name + "!";
    }

    // 12. Return raw object without ResponseEntity wrapper
    @GetMapping(path = "/users/{userId}/info")
    public UserInfo getUserInfo(@PathParam String userId) {
        return new UserInfo(userId, "User " + userId);
    }

    // Simple record for testing
    public record UserInfo(String id, String name) {}
}
