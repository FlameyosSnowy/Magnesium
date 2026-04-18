import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.http.response.Response;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.http.websocket.WebSocketFrame;
import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.HttpFilterChain;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Advanced Magnesium Application Example
 *
 * Demonstrates:
 * - Async request handling
 * - Custom filters and middleware
 * - WebSocket support
 * - Request/response interceptors
 * - Advanced routing patterns
 */
public class AdvancedApplicationExample extends Application {

    public static class OrderService {
        private final Map<String, Order> orders = new ConcurrentHashMap<>();

        public Order create(String product, int quantity, double price) {
            String id = java.util.UUID.randomUUID().toString();
            Order order = new Order(id, product, quantity, price, "PENDING");
            orders.put(id, order);
            return order;
        }

        public Order find(String id) {
            return orders.get(id);
        }

        public void updateStatus(String id, String status) {
            Order order = orders.get(id);
            if (order != null) {
                order.status = status;
            }
        }

        public void warmUp() {
            System.out.println("[OrderService] Pre-loading product catalog...");
        }
    }

    public static class AuditService {
        public void log(String action, String details) {
            System.out.printf("[AUDIT] %s: %s%n", action, details);
        }
    }

    public static class Order {
        public String id;
        public String product;
        public int quantity;
        public double price;
        public String status;

        public Order(String id, String product, int quantity, double price, String status) {
            this.id = id;
            this.product = product;
            this.quantity = quantity;
            this.price = price;
            this.status = status;
        }
    }

    /**
     * Timing filter - adds X-Response-Time header
     */
    public static class TimingFilter implements HttpFilter {
        @Override
        public ResponseEntity<?> handle(RequestContext ctx, HttpFilterChain next) {
            long start = System.nanoTime();
            try {
                return next.next(ctx);
            } finally {
                long duration = (System.nanoTime() - start) / 1_000_000; // ms
                ctx.response().header("X-Response-Time", duration + "ms");
            }
        }
    }

    /**
     * CORS filter - handles cross-origin requests
     */
    public static class CorsFilter implements HttpFilter {
        @Override
        public ResponseEntity<?> handle(RequestContext ctx, HttpFilterChain next) {
            // Add CORS headers to all responses
            ctx.response()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization");

            // Handle preflight
            if (ctx.request().method().name().equals("OPTIONS")) {
                return Response.ok().build();
            }

            return next.next(ctx);
        }
    }

    /**
     * Rate limiting filter - simple in-memory implementation
     */
    public static class RateLimitFilter implements HttpFilter {
        private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();
        private final int maxRequests = 100;

        @Override
        public ResponseEntity<?> handle(RequestContext ctx, HttpFilterChain next) {
            String clientIp = ctx.header("X-Forwarded-For");
            if (clientIp == null) {
                clientIp = "unknown";
            }

            int count = requestCounts.merge(clientIp, 1, Integer::sum);

            if (count > maxRequests) {
                return Response.status(429)
                    .body(Map.of("error", "Rate limit exceeded"))
                    .header("Retry-After", "60");
            }

            return next.next(ctx);
        }
    }

    /**
     * JWT Authentication filter
     */
    public static class JwtAuthFilter implements HttpFilter {
        @Override
        public ResponseEntity<?> handle(RequestContext ctx, HttpFilterChain next) {
            String authHeader = ctx.header("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                    .body(Map.of("error", "Missing or invalid token"));
            }

            String token = authHeader.substring(7);

            if (!isValidToken(token)) {
                return Response.status(401)
                    .body(Map.of("error", "Invalid token"));
            }

            // Attach user info to context
            ctx.attribute("userId", extractUserId(token));

            return next.next(ctx);
        }

        private boolean isValidToken(String token) {
            // Real implementation would verify JWT signature
            return token.length() > 10;
        }

        private String extractUserId(String token) {
            // Real implementation would decode JWT payload
            return "user_123";
        }
    }

    public static class ChatWebSocketHandler implements WebSocketHandler {
        private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

        @Override
        public void onOpen(WebSocketSession session) {
            sessions.put(session.id(), session);
            broadcast("User joined: " + session.id());
        }

        @Override
        public void onMessage(WebSocketSession session, WebSocketFrame frame) {
            if (frame.isText()) {
                String message = frame.textData();
                broadcast("[" + session.id() + "]: " + message);
            }
        }

        @Override
        public void onClose(WebSocketSession session, int statusCode, String reason) {
            sessions.remove(session.id());
            broadcast("User left: " + session.id());
        }

        @Override
        public void onError(WebSocketSession session, Throwable error) {
            System.err.println("WebSocket error: " + error.getMessage());
        }

        private void broadcast(String message) {
            sessions.values().forEach(session -> {
                session.sendText(message);
            });
        }
    }

    @Override
    protected void configure(MagnesiumRuntime runtime) {
        runtime.executor(Executors.newVirtualThreadPerTaskExecutor());
        runtime.requestTimeout(Duration.ofSeconds(30));

        runtime.services(s -> s
            .register(OrderService.class, ctx -> new OrderService())
            .register(AuditService.class, ctx -> new AuditService())
        );

        runtime.router().filter(new CorsFilter());

        SecurityHeadersFilter security = new SecurityHeadersFilter();
        runtime.securityHeadersFilter(security);

        runtime.router()
            // Public routes (no auth)
            .get("/api/health", ctx -> Response.ok(Map.of(
                "status", "healthy",
                "version", "2.0.0",
                "timestamp", java.time.Instant.now().toString()
            )))

            // Protected API routes
            .get("/api/orders/:id", ctx -> {
                OrderService service = runtime.serviceRegistry().get(OrderService.class);
                String orderId = ctx.pathParam("id");
                Order order = service.find(orderId);

                if (order == null) {
                    return Response.status(404).body(Map.of("error", "Order not found"));
                }

                return Response.ok(order);
            })
            .filter(new JwtAuthFilter())
            .filter(new TimingFilter())
            .register()

            .post("/api/orders", ctx -> {
                OrderService orderService = runtime.serviceRegistry().get(OrderService.class);
                AuditService auditService = runtime.serviceRegistry().get(AuditService.class);

                return CompletableFuture.supplyAsync(() -> {
                    Order order = orderService.create("Product-ABC", 5, 99.99);
                    auditService.log("ORDER_CREATED", order.id);
                    return Response.status(201).body(order);
                }, runtime.executor());
            })
            .filter(new JwtAuthFilter())
            .filter(new RateLimitFilter())
            .register()

            .get("/api/admin/metrics", ctx -> Response.ok(Map.of(
                "totalOrders", 150,
                "activeUsers", 42,
                "serverUptime", System.currentTimeMillis()
            )))
            .filter(ctx -> {
                String userId = ctx.attribute("userId");
                if (!isAdmin(userId)) {
                    return Response.status(403).body(Map.of("error", "Admin access required"));
                }
                return ctx.proceed();
            })
            .register()

            // WebSocket endpoint
            .websocket("/ws/chat", new ChatWebSocketHandler());

        runtime.exceptions(ex -> ex
            .global(java.util.concurrent.TimeoutException.class, (error, req) ->
                Response.status(504).body(Map.of("error", "Request timeout")))
            .global(IllegalStateException.class, (error, req) ->
                Response.status(409).body(Map.of("error", error.getMessage())))
            .fallback((error, req) -> {
                error.printStackTrace();
                return Response.status(500).body(Map.of(
                    "error", "Internal server error",
                    "requestId", req.header("X-Request-Id")
                ));
            })
        );
    }

    private boolean isAdmin(String userId) {
        return userId != null && userId.startsWith("admin_");
    }

    @Override
    protected void initialize(MagnesiumRuntime runtime) throws Exception {
        OrderService orderService = runtime.serviceRegistry().get(OrderService.class);
        orderService.warmUp();

        System.out.println("Application initialized");
    }

    @Override
    protected void ready(MagnesiumRuntime runtime, int port) throws Exception {
        System.out.println("Server ready on http://localhost:" + port);
        System.out.println("  - API docs: http://localhost:" + port + "/api/health");
        System.out.println("  - WebSocket: ws://localhost:" + port + "/ws/chat");
    }

    @Override
    protected void stop(MagnesiumRuntime runtime) throws Exception {
        System.out.println("Shutting down gracefully...");
    }

    public static void main(String[] args) {
        MagnesiumApplication.run(new AdvancedApplicationExample(), 8080);
    }
}
