import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.StartContext;
import net.magnesiumbackend.core.http.response.Response;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.services.ServiceContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Basic Magnesium Application Example
 *
 * This demonstrates the typical structure of a Magnesium application using
 * the Application -> MagnesiumRuntime -> MagnesiumApplication.run() flow.
 */
public class BasicApplicationExample extends Application {

    public static class UserService {
        public User findById(String id) {
            // Simulate database lookup
            return new User(id, "Alice", "alice@example.com");
        }

        public User create(String name, String email) {
            return new User(java.util.UUID.randomUUID().toString(), name, email);
        }

        public void warmUp() {
            System.out.println("[UserService] Warming up connection pool...");
        }

        public void shutdown() {
            System.out.println("[UserService] Shutting down gracefully...");
        }
    }

    public static class User {
        public final String id;
        public final String name;
        public final String email;

        public User(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }

    public static class UserController {
        private final UserService userService;

        public UserController(UserService userService) {
            this.userService = userService;
        }

        public ResponseEntity getUser(RequestContext ctx) {
            String userId = ctx.pathParam("id");
            User user = userService.findById(userId);
            return Response.ok(user);
        }

        public CompletableFuture<ResponseEntity> listUsers(RequestContext ctx) {
            // Async handler example
            return CompletableFuture.completedFuture(
                Response.ok(new User[]{new User("1", "Alice", "a@b.com")})
            );
        }
    }

    @Override
    protected void configure(MagnesiumRuntime runtime) {
        runtime.executor(Executors.newVirtualThreadPerTaskExecutor());

        // Configure JSON provider (Jackson, DSL-JSON, etc.)
        // runtime.json(new JacksonJsonProvider());

        runtime.services(s -> s
            .register(UserService.class, ctx -> {
                return new UserService();
            })
        );

        runtime.router()
            .get("/health", req -> Response.ok("up"))

            .get("/api/status", req -> Response.ok()
                .header("X-Version", "1.0.0")
                .body(java.util.Map.of(
                    "status", "healthy",
                    "timestamp", System.currentTimeMillis()
                )))

            .get("/api/users/:id", ctx -> {
                UserService service = runtime.serviceRegistry().get(UserService.class);
                return new UserController(service).getUser(ctx);
            })

            .post("/api/users", ctx -> {
                UserService service = runtime.serviceRegistry().get(UserService.class);
                User created = service.create("New User", "new@example.com");
                return Response.status(201).body(created);
            })

            .get("/api/admin/stats", ctx -> Response.ok("admin data"))
            .filter(ctx -> {
                // Simple auth check
                String auth = ctx.header("Authorization");
                if (auth == null) {
                    return Response.status(401).body("Unauthorized");
                }
                return ctx.proceed();
            })
            .register();


        runtime.exceptions(ex -> ex
            .global(IllegalArgumentException.class, (error, req) ->
                Response.status(400).body(error.getMessage()))
            .fallback((error, req) ->
                Response.status(500).body("Internal Server Error: " + error.getMessage()))
        );

        // runtime.backpressure(bp -> bp
        //     .queueCapacity(1000)
        //     .onReject(RejectionResponse.of(503)
        //         .withBody("Server busy"))
        // );

        // runtime.requestSecurity(sec -> sec
        //     .signRequests()
        //     .withAlgorithm("HmacSHA256")
        // );

        // runtime.ssl(SslConfig.builder()
        //     .certificatePath("/path/to/cert.pem")
        //     .privateKeyPath("/path/to/key.pem")
        //     .build()
        // );
    }

    @Override
    protected void initialize(MagnesiumRuntime runtime) throws Exception {
        UserService userService = runtime.serviceRegistry().get(UserService.class);
        userService.warmUp();

        System.out.println("[Application] Initialization complete");
    }

    @Override
    protected void ready(MagnesiumRuntime runtime, int port) throws Exception {
        // Called after server is bound and accepting connections
        // Good for: service discovery registration, health signals

        System.out.println("[Application] Server ready on port " + port);

        // Example: Register with service discovery
        // consulClient.register("my-service", port);
    }

    @Override
    protected void stop(MagnesiumRuntime runtime) throws Exception {
        // Called on graceful shutdown (SIGTERM/SIGINT)
        // Good for: draining connections, flushing buffers

        UserService userService = runtime.serviceRegistry().get(UserService.class);
        userService.shutdown();

        System.out.println("[Application] Shutdown complete");
    }

    public static void main(String[] args) {
        // Start the application on port 8080
        MagnesiumApplication.run(new BasicApplicationExample(), 8080);
    }
}
