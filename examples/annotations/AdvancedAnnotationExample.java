import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.annotations.*;
import net.magnesiumbackend.core.annotations.RateLimit.Algorithm;
import net.magnesiumbackend.core.annotations.RateLimit.KeyResolverType;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.http.websocket.WebSocketFrame;
import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.HttpFilterChain;
import net.magnesiumbackend.core.route.RequestContext;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Advanced Annotation-Based Magnesium Application Example
 *
 * Demonstrates:
 * - Dependency injection with @RestService and @Inject
 * - Service interface registration with RestService.as()
 * - Service lifecycle with @OnInitialize and @PreDestroy
 * - Async service initialization with RestService.async()
 * - Multiple HTTP methods (GET, POST, PUT, DELETE)
 * - Custom filters with @Filter
 * - Rate limiting with @RateLimit
 * - Authentication/authorization with @Authenticated and @Requires
 * - WebSocket support with @WebSocketMapping
 * - Exception handling with @ExceptionHandler
 * - Configuration with @ApplicationConfiguration
 *
 * Needs a JsonProvider such as jackson for proper JSON serialization and deserialization of requests and responses
 */
public class AdvancedAnnotationExample extends Application {

    @ApplicationConfiguration
    public record DatabaseConfig(
        @ConfigKey("db.host") String host,
        @ConfigKey("db.port") int port,
        @ConfigKey("db.name") String database
    ) {}

    @ApplicationConfiguration
    public record ServerConfig(
        @ConfigKey("server.request-timeout-seconds") int requestTimeoutSeconds,
        @ConfigKey("server.max-connections") int maxConnections
    ) {}

    public record Order(UUID id, String product, int quantity, double price, String status) {
        public Order(String product, int quantity, double price) {
            this(UUID.randomUUID(), product, quantity, price, "PENDING");
        }
    }

    public record CreateOrderRequest(String product, int quantity, double price) {}
    public record UpdateStatusRequest(String status) {}

    public static class TimingFilter implements HttpFilter {
        @Override
        public ResponseEntity<?> handle(RequestContext ctx, HttpFilterChain next) {
            long start = System.nanoTime();
            try {
                return next.next(ctx);
            } finally {
                long duration = (System.nanoTime() - start) / 1_000_000;
                ctx.response().header("X-Response-Time", duration + "ms");
            }
        }
    }

    public static class CorsFilter implements HttpFilter {
        @Override
        public ResponseEntity<?> handle(RequestContext ctx, HttpFilterChain next) {
            ctx.response()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equals(ctx.request().method().name())) {
                return ResponseEntity.ok().build();
            }
            return next.next(ctx);
        }
    }

    public static class RequestIdFilter implements HttpFilter {
        @Override
        public ResponseEntity<?> handle(RequestContext ctx, HttpFilterChain next) {
            String requestId = ctx.header("X-Request-Id");
            if (requestId == null) {
                requestId = UUID.randomUUID().toString();
            }
            ctx.attribute("requestId", requestId);
            ctx.response().header("X-Request-Id", requestId);
            return next.next(ctx);
        }
    }

    @RestService
    @Lifecycle(stage = LifecycleStage.INIT)
    public static class OrderService {
        private final Map<UUID, Order> orders = new ConcurrentHashMap<>();

        public Order create(String product, int quantity, double price) {
            Order order = new Order(product, quantity, price);
            orders.put(order.id(), order);
            return order;
        }

        public Order find(UUID id) {
            return orders.get(id);
        }

        public List<Order> findAll() {
            return List.copyOf(orders.values());
        }

        public void updateStatus(UUID id, String status) {
            Order existing = orders.get(id);
            if (existing != null) {
                orders.put(id, new Order(id, existing.product(), existing.quantity(), existing.price(), status));
            }
        }

        @OnInitialize
        public void warmUp() {
            System.out.println("[OrderService] Pre-loading sample orders...");
            create("Product-A", 5, 99.99);
            create("Product-B", 3, 149.99);
        }

        @PreDestroy
        public void shutdown() {
            System.out.println("[OrderService] Flushing orders to persistent storage...");
            orders.clear();
        }
    }

    // Example of interface-based service registration with 'as()' attribute
    public interface AuditLogger {
        void log(String action, String details);
    }

    @RestService(as = AuditLogger.class)
    public static class AuditService implements AuditLogger {
        @Override
        public void log(String action, String details) {
            System.out.printf("[AUDIT] %s: %s%n", action, details);
        }
    }

    @RestService
    public static class NotificationService {
        public void sendOrderNotification(UUID orderId, String event) {
            System.out.printf("[NOTIFICATION] Order %s: %s%n", orderId, event);
        }
    }

    @RestController
    @Filter(CorsFilter.class)  // Applied to all routes in this controller
    @Filter(RequestIdFilter.class)
    public static class OrderController {
        private final OrderService orderService;
        private final AuditLogger auditLogger;
        private final NotificationService notificationService;

        public OrderController(OrderService orderService, AuditLogger auditLogger,
                               @Inject(optional = true) NotificationService notificationService) {
            this.orderService = orderService;
            this.auditLogger = auditLogger;
            this.notificationService = notificationService;
        }

        // Public health endpoint - no auth required
        @GetMapping(path = "/api/health")
        public ResponseEntity<Map<String, Object>> health() {
            return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "version", "2.0.0",
                "timestamp", java.time.Instant.now().toString()
            ));
        }

        // List all orders
        @GetMapping(path = "/api/orders")
        public CompletableFuture<ResponseEntity<List<Order>>> listOrders() {
            return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(orderService.findAll())
            );
        }

        // Get single order with path param
        @GetMapping(path = "/api/orders/{orderId}")
        @Filter(TimingFilter.class)
        public ResponseEntity<?> getOrder(@PathParam UUID orderId, RequestContext ctx) {
            Order order = orderService.find(orderId);
            if (order == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
            }
            auditLogger.log("ORDER_VIEWED", orderId.toString());
            return ResponseEntity.ok(order);
        }

        // Create order - protected with rate limiting
        @PostMapping(path = "/api/orders")
        @Authenticated
        @RateLimit(requests = 10, windowSeconds = 60, algorithm = Algorithm.TOKEN_BUCKET)
        @ResponseStatus(201)
        public ResponseEntity<Order> createOrder(
                CreateOrderRequest request,
                RequestContext ctx) {
            Order order = orderService.create(
                request.product(), request.quantity(), request.price());

            auditLogger.log("ORDER_CREATED", order.id().toString());

            if (notificationService != null) {
                notificationService.sendOrderNotification(order.id(), "CREATED");
            }

            return ResponseEntity.status(201).body(order);
        }

        // Update order status
        @PutMapping(path = "/api/orders/{orderId}/status")
        @Authenticated
        public ResponseEntity<?> updateStatus(
                @PathParam UUID orderId,
                UpdateStatusRequest request,
                RequestContext ctx) {
            if (orderService.find(orderId) == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
            }
            orderService.updateStatus(orderId, request.status());
            auditLogger.log("ORDER_STATUS_UPDATED", orderId + " -> " + request.status());
            return ResponseEntity.ok(Map.of("id", orderId, "status", request.status()));
        }

        // Delete order - admin only
        @DeleteMapping(path = "/api/orders/{orderId}")
        @Authenticated
        @Requires("admin")
        public ResponseEntity<?> deleteOrder(@PathParam UUID orderId, RequestContext ctx) {
            // In real app, would delete from database
            auditLogger.log("ORDER_DELETED", orderId.toString());
            return ResponseEntity.noContent().build();
        }
    }

    @RestController
    @Authenticated
    @Requires("admin")
    @Filter(TimingFilter.class)
    public static class AdminController {

        @GetMapping(path = "/api/admin/metrics")
        public ResponseEntity<Map<String, Object>> metrics() {
            return ResponseEntity.ok(Map.of(
                "totalOrders", 150,
                "activeUsers", 42,
                "serverUptime", System.currentTimeMillis()
            ));
        }

        @GetMapping(path = "/api/admin/logs")
        public ResponseEntity<List<Map<String, String>>> logs(
                @QueryParam(value = "level", required = false, defaultValue = "INFO") String level) {
            return ResponseEntity.ok(List.of(
                Map.of("level", level, "message", "Sample log entry")
            ));
        }
    }

    @RestController
    public static class ChatController {
        private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

        @WebSocketMapping(path = "/ws/chat")
        public WebSocketHandler handleChat() {
            return new WebSocketHandler() {
                @Override
                public void onOpen(WebSocketSession session) {
                    sessions.put(session.id(), session);
                    broadcast("User joined: " + session.id());
                }

                @Override
                public void onMessage(WebSocketSession session, WebSocketFrame frame) {
                    if (frame.isText()) {
                        broadcast("[" + session.id() + "]: " + frame.textData());
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
            };
        }

        private void broadcast(String message) {
            sessions.values().forEach(s -> s.sendText(message));
        }
    }

    @ExceptionHandler
    public static class GlobalExceptionHandler {

        @net.magnesiumbackend.core.annotations.ExceptionHandler(java.util.concurrent.TimeoutException.class)
        public ResponseEntity<Map<String, String>> handleTimeout(java.util.concurrent.TimeoutException e) {
            return ResponseEntity.status(504).body(Map.of("error", "Request timeout"));
        }

        @net.magnesiumbackend.core.annotations.ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @Override
    protected void configure(MagnesiumRuntime runtime) {
        runtime.executor(Executors.newVirtualThreadPerTaskExecutor());
        runtime.requestTimeout(Duration.ofSeconds(30));
    }

    @Override
    protected void initialize(MagnesiumRuntime runtime) throws Exception {
        System.out.println("[Application] Initializing...");
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
        MagnesiumApplication.run(new AdvancedAnnotationExample(), 8080);
    }
}
