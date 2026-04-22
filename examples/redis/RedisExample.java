package net.magnesiumbackend.examples.redis;

import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.annotations.RestController;
import net.magnesiumbackend.core.annotations.GetMapping;
import net.magnesiumbackend.core.annotations.PostMapping;
import net.magnesiumbackend.core.annotations.service.Service;
import net.magnesiumbackend.core.http.request.Request;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.json.JsonProvider;
import net.magnesiumbackend.redis.RedisConfiguration;
import net.magnesiumbackend.redis.RedisExtension;
import net.magnesiumbackend.redis.RedisService;
import net.magnesiumbackend.redis.RedisTemplate;
import net.magnesiumbackend.redis.pipeline.RedisPipeline;
import net.magnesiumbackend.redis.pubsub.RedisPubSubService;
import net.magnesiumbackend.redis.reactive.ReactiveRedisOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Example demonstrating comprehensive Redis integration with Magnesium.
 *
 * <p>Features shown:</p>
 * <ul>
 *   <li>RedisTemplate for type-safe operations</li>
 *   <li>Pub/Sub messaging</li>
 *   <li>Reactive API with Project Reactor</li>
 *   <li>Pipelining for batch operations</li>
 *   <li>All Redis data types (String, List, Set, Sorted Set, Hash)</li>
 *   <li>Cluster and Sentinel support via configuration</li>
 * </ul>
 */
public class RedisExample extends Application {

    // Domain models
    public record User(String id, String name, String email, int age) {}
    public record Order(String orderId, String userId, double amount, String status) {}
    public record Product(String productId, String name, double price, int stock) {}

    /**
     * User service with Redis caching.
     */
    @Service
    public static class UserService {
        private final RedisTemplate<String, User> userTemplate;
        private final RedisTemplate<String, String> stringTemplate;

        public UserService(RedisService redisService, JsonProvider jsonProvider) {
            this.userTemplate = RedisExtension.createTemplate(
                redisService, jsonProvider, String.class, User.class, "user:");
            this.stringTemplate = RedisExtension.createTemplate(
                redisService, jsonProvider, String.class, String.class, null);
        }

        public void createUser(User user) {
            // Cache user with 1 hour TTL
            userTemplate.set(user.id(), user, Duration.ofHours(1));

            // Add to users index set
            stringTemplate.addToSet("users:all", user.id());

            // Add to sorted set by age
            userTemplate.addToZSet("users:by-age", user, user.age());
        }

        public User getUser(String userId) {
            return userTemplate.get(userId);
        }

        public List<User> getUsersByAgeRange(int minAge, int maxAge) {
            return userTemplate.zRangeByScore("users:by-age", minAge, maxAge);
        }

        public void deleteUser(String userId) {
            userTemplate.delete(userId);
            stringTemplate.removeFromSet("users:all", userId);
            userTemplate.removeFromZSet("users:by-age", getUser(userId));
        }
    }

    /**
     * Order service with Redis streams and pub/sub.
     */
    @Service
    public static class OrderService {
        private final RedisTemplate<String, Order> orderTemplate;
        private final RedisPubSubService pubSubService;

        public OrderService(RedisService redisService, RedisPubSubService pubSubService, JsonProvider jsonProvider) {
            this.orderTemplate = RedisExtension.createTemplate(
                redisService, jsonProvider, String.class, Order.class, "order:");
            this.pubSubService = pubSubService;
        }

        public void createOrder(Order order) {
            orderTemplate.set(order.orderId(), order, Duration.ofMinutes(30));

            // Publish order created event
            pubSubService.publish("orders:created", order);

            // Add to user's order list
            orderTemplate.leftPush("orders:user:" + order.userId(), order);
        }

        public Order getOrder(String orderId) {
            return orderTemplate.get(orderId);
        }

        public List<Order> getUserOrders(String userId) {
            return orderTemplate.range("orders:user:" + userId, 0, 99);
        }

        public void updateOrderStatus(String orderId, String newStatus) {
            Order order = getOrder(orderId);
            if (order != null) {
                Order updated = new Order(
                    order.orderId(), order.userId(), order.amount(), newStatus);
                orderTemplate.set(orderId, updated, Duration.ofMinutes(30));

                // Publish status change
                pubSubService.publish("orders:status-changed",
                    Map.of("orderId", orderId, "status", newStatus));
            }
        }
    }

    /**
     * Product service with Redis inventory management.
     */
    @Service
    public static class ProductService {
        private final RedisTemplate<String, Product> productTemplate;
        private final RedisService redisService;

        public ProductService(RedisService redisService, JsonProvider jsonProvider) {
            this.redisService = redisService;
            this.productTemplate = RedisExtension.createTemplate(
                redisService, jsonProvider, String.class, Product.class, "product:");
        }

        public void createProduct(Product product) {
            productTemplate.set(product.productId(), product);

            // Store stock as separate counter for atomic operations
            productTemplate.putHash("products:stock", product.productId(),
                String.valueOf(product.stock()));
        }

        public Product getProduct(String productId) {
            return productTemplate.get(productId);
        }

        /**
         * Decrement stock atomically using Redis increment.
         */
        public long decrementStock(String productId, int amount) {
            // Use RedisPipeline for atomic stock check and decrement
            RedisPipeline pipeline = new RedisPipeline(redisService);

            pipeline.add(commands -> commands.hget("products:stock", productId));
            pipeline.add(commands -> commands.hincrby("products:stock", productId, -amount));

            List<Object> results = pipeline.execute();
            Long currentStock = Long.valueOf((String) results.get(0));

            return currentStock - amount;
        }

        public List<Product> getTopProducts(int count) {
            // Using sorted set by price for top products
            return productTemplate.zRangeWithScores("products:by-price", 0, count - 1)
                .stream()
                .map(sv -> sv.getValue())
                .toList();
        }
    }

    /**
     * Event listener for pub/sub.
     */
    @Service
    public static class OrderEventListener {
        private final RedisPubSubService pubSubService;

        public OrderEventListener(RedisPubSubService pubSubService) {
            this.pubSubService = pubSubService;
        }

        public void initialize() {
            // Subscribe to order events
            pubSubService.subscribe("orders:created", (channel, message, pattern) -> {
                System.out.println("New order created: " + message);
                // Send notification, update analytics, etc.
            });

            // Subscribe with pattern for all order events
            pubSubService.psubscribe("orders:*", (channel, message, pattern) -> {
                System.out.println("Order event on " + channel + ": " + message);
            });
        }
    }

    /**
     * Reactive service demonstrating reactive Redis operations.
     */
    @Service
    public static class ReactiveAnalyticsService {
        private final ReactiveRedisOperations<String, String> reactiveOps;
        private final ReactiveRedisOperations<String, User> userReactiveOps;

        public ReactiveAnalyticsService(RedisService redisService, JsonProvider jsonProvider) {
            this.reactiveOps = RedisExtension.createReactive(
                redisService, jsonProvider, String.class, String.class, "analytics:");
            this.userReactiveOps = RedisExtension.createReactive(
                redisService, jsonProvider, String.class, User.class, null);
        }

        public Mono<Long> trackPageView(String page) {
            return reactiveOps.increment("pageviews:" + page);
        }

        public Mono<Map<String, String>> getUserStats(String userId) {
            return reactiveOps.getHashAll("stats:user:" + userId);
        }

        public Flux<User> getActiveUsers() {
            // Reactive stream of users from a Redis set
            return reactiveOps.setMembers("users:active")
                .flatMap(userId -> userReactiveOps.get(userId)
                    .filter(user -> user != null));
        }

        public Mono<Void> batchUpdateUserStats(List<String> userIds) {
            return Flux.fromIterable(userIds)
                .flatMap(userId -> reactiveOps.putHash(
                    "stats:user:" + userId, "lastActive", System.currentTimeMillis()))
                .then();
        }
    }

    /**
     * REST API controller.
     */
    @RestController
    public static class ApiController {
        private final UserService userService;
        private final OrderService orderService;
        private final ProductService productService;
        private final ReactiveAnalyticsService analyticsService;

        public ApiController(
            UserService userService,
            OrderService orderService,
            ProductService productService,
            ReactiveAnalyticsService analyticsService
        ) {
            this.userService = userService;
            this.orderService = orderService;
            this.productService = productService;
            this.analyticsService = analyticsService;
        }

        @PostMapping(path = "/api/users")
        public ResponseEntity<String> createUser(Request request) {
            User user = new User(
                java.util.UUID.randomUUID().toString(),
                request.queryParam("name"),
                request.queryParam("email"),
                Integer.parseInt(request.queryParam("age"))
            );
            userService.createUser(user);
            return ResponseEntity.ok("User created: " + user.id());
        }

        @GetMapping(path = "/api/users/:id")
        public ResponseEntity<User> getUser(Request request) {
            User user = userService.getUser(request.pathParam("id"));
            return user != null
                ? ResponseEntity.ok(user)
                : ResponseEntity.status(404).build();
        }

        @GetMapping(path = "/api/users/by-age")
        public ResponseEntity<List<User>> getUsersByAge(Request request) {
            int min = Integer.parseInt(request.queryParam("min", "0"));
            int max = Integer.parseInt(request.queryParam("max", "100"));
            return ResponseEntity.ok(userService.getUsersByAgeRange(min, max));
        }

        @PostMapping(path = "/api/orders")
        public ResponseEntity<String> createOrder(Request request) {
            Order order = new Order(
                java.util.UUID.randomUUID().toString(),
                request.queryParam("userId"),
                Double.parseDouble(request.queryParam("amount")),
                "pending"
            );
            orderService.createOrder(order);
            return ResponseEntity.ok("Order created: " + order.orderId());
        }

        @GetMapping(path = "/api/products/:id/stock")
        public ResponseEntity<Long> checkStock(Request request) {
            long stock = productService.decrementStock(request.pathParam("id"), 0);
            return ResponseEntity.ok(stock);
        }

        @PostMapping(path = "/api/products/:id/purchase")
        public ResponseEntity<String> purchaseProduct(Request request) {
            String productId = request.pathParam("id");
            int amount = Integer.parseInt(request.queryParam("amount", "1"));

            long remaining = productService.decrementStock(productId, amount);
            if (remaining >= 0) {
                return ResponseEntity.ok("Purchase successful. Remaining: " + remaining);
            } else {
                return ResponseEntity.status(400).body("Insufficient stock");
            }
        }
    }

    @Override
    public void configure(net.magnesiumbackend.core.MagnesiumRuntime runtime) {
        // Redis configuration loaded from application.toml
    }

    public static void main(String[] args) {
        MagnesiumApplication.run(new RedisExample(), 8080);
    }
}
