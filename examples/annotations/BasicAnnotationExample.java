import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.annotations.*;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Basic Annotation-Based Magnesium Application Example
 *
 * This demonstrates the typical structure of a Magnesium application using
 * annotations for dependency injection, service registration, and routing.
 */
public class BasicAnnotationExample extends Application {

    // ============================================================
    // Domain Models
    // ============================================================

    public record User(String id, String name, String email) {}
    public record CreateUserRequest(String name, String email) {}

    // ============================================================
    // Services (Auto-registered with @RestService)
    // ============================================================

    @RestService
    public static class UserService {
        private final java.util.Map<String, User> users = new java.util.concurrent.ConcurrentHashMap<>();

        public User findById(String id) {
            return users.get(id);
        }

        public List<User> findAll() {
            return List.copyOf(users.values());
        }

        public User create(String name, String email) {
            String id = java.util.UUID.randomUUID().toString();
            User user = new User(id, name, email);
            users.put(id, user);
            return user;
        }

        @OnInitialize
        public void warmUp() {
            System.out.println("[UserService] Warming up...");
            // Pre-populate with sample data
            create("Alice", "alice@example.com");
            create("Bob", "bob@example.com");
        }

        @PreDestroy
        public void shutdown() {
            System.out.println("[UserService] Shutting down gracefully...");
        }
    }

    @RestService
    public static class AuditService {
        public void log(String action, String details) {
            System.out.printf("[AUDIT] %s: %s%n", action, details);
        }
    }

    // ============================================================
    // REST Controller (Auto-registered routes via annotations)
    // ============================================================

    @RestController
    public static class UserController {
        private final UserService userService;
        private final AuditService auditService;

        // Constructor injection - dependencies auto-resolved
        public UserController(UserService userService, AuditService auditService) {
            this.userService = userService;
            this.auditService = auditService;
        }

        // Health check endpoint
        @GetMapping(path = "/health")
        public ResponseEntity<java.util.Map<String, Object>> health() {
            return ResponseEntity.ok(java.util.Map.of(
                "status", "healthy",
                "timestamp", System.currentTimeMillis()
            ));
        }

        // Async handler example
        @GetMapping(path = "/api/users")
        public CompletableFuture<ResponseEntity<List<User>>> listUsers() {
            return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(userService.findAll())
            );
        }

        // Path parameter with automatic type conversion
        @GetMapping(path = "/api/users/{id}")
        public ResponseEntity<?> getUser(@PathParam String id) {
            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.status(404).body(java.util.Map.of("error", "User not found"));
            }
            auditService.log("USER_VIEWED", id);
            return ResponseEntity.ok(user);
        }

        // POST with custom response status
        @PostMapping(path = "/api/users")
        @ResponseStatus(201)
        public ResponseEntity<User> createUser(CreateUserRequest request) {
            User created = userService.create(request.name(), request.email());
            auditService.log("USER_CREATED", created.id());
            return ResponseEntity.status(201).body(created);
        }

        // Query parameters example
        @GetMapping(path = "/api/search")
        public ResponseEntity<java.util.Map<String, String>> search(
                @QueryParam(value = "q") String query,
                @QueryParam(value = "limit", required = false, defaultValue = "10") int limit) {
            return ResponseEntity.ok(java.util.Map.of(
                "query", query,
                "limit", String.valueOf(limit)
            ));
        }
    }

    // ============================================================
    // Admin Controller with authentication requirement
    // ============================================================

    @RestController
    @Authenticated  // All routes require authentication
    public static class AdminController {

        @GetMapping(path = "/api/admin/stats")
        public ResponseEntity<java.util.Map<String, Object>> stats() {
            return ResponseEntity.ok(java.util.Map.of(
                "totalUsers", 100,
                "activeSessions", 42,
                "serverUptime", System.currentTimeMillis()
            ));
        }
    }

    // ============================================================
    // Application Configuration
    // ============================================================

    @Override
    protected void configure(MagnesiumRuntime runtime) {
        // Virtual thread executor for async operations
        runtime.executor(Executors.newVirtualThreadPerTaskExecutor());

        // Exception handling
        runtime.exceptions(ex -> ex
            .global(IllegalArgumentException.class, (error, req) ->
                ResponseEntity.status(400).body(error.getMessage()))
            .fallback((error, req) ->
                ResponseEntity.status(500).body("Internal Server Error: " + error.getMessage()))
        );
    }

    @Override
    protected void ready(MagnesiumRuntime runtime, int port) throws Exception {
        System.out.println("[Application] Server ready on port " + port);
        System.out.println("  - Health: http://localhost:" + port + "/health");
        System.out.println("  - API: http://localhost:" + port + "/api/users");
    }

    public static void main(String[] args) {
        MagnesiumApplication.run(new BasicAnnotationExample(), 8080);
    }
}
