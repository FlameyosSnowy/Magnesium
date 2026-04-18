package net.magnesiumbackend.core.annotations;

import java.lang.annotation.*;

/**
 * Declares a dependency on another service with optional resilience support.
 *
 * <p>Unlike the {@code dependsOn} attribute in {@link Lifecycle}, this annotation:
 * <ul>
 *   <li>Can be used multiple times on the same class</li>
 *   <li>Supports optional dependencies for microservice resilience</li>
 *   <li>Allows custom timeout and retry configuration per dependency</li>
 *   <li>Supports graceful degradation when dependencies are unavailable</li>
 * </ul>
 *
 * <h3>Basic Usage (Required Dependency)</h3>
 * <pre>{@code
 * @RestService
 * @Lifecycle(stage = LifecycleStage.INIT)
 * @DependsOn(DatabaseService.class)
 * public class UserRepository {
 *     private final DatabaseService db;
 *
 *     public UserRepository(DatabaseService db) {
 *         this.db = db;
 *     }
 * }
 * }</pre>
 *
 * <h3>Multiple Dependencies</h3>
 * <pre>{@code
 * @RestService
 * @Lifecycle(stage = LifecycleStage.POST_INIT)
 * @DependsOn(DatabaseService.class)
 * @DependsOn(CacheService.class)
 * public class ProductService {
 *     private final DatabaseService db;
 *     private final CacheService cache;
 *
 *     public ProductService(DatabaseService db, CacheService cache) {
 *         this.db = db;
 *         this.cache = cache;
 *     }
 * }
 * }</pre>
 *
 * <h3>Optional Dependencies (Microservice Resilience)</h3>
 * <pre>{@code
 * @RestService
 * @Lifecycle(stage = LifecycleStage.READY)
 * @DependsOn(value = DatabaseService.class)  // Required - startup fails without it
 * @DependsOn(value = NotificationService.class, optional = true, timeout = 5000)
 * @DependsOn(value = AnalyticsService.class, optional = true, retry = 3)
 * @DependsOn(value = AuditService.class, optional = true)
 * public class OrderService {
 *     private final DatabaseService db;
 *     private final NotificationService notifications;
 *     private final AnalyticsService analytics;
 *     private final AuditService audit;
 *
 *     public OrderService(
 *         DatabaseService db,
 *         @Inject(optional = true) NotificationService notifications,
 *         @Inject(optional = true) AnalyticsService analytics,
 *         @Inject(optional = true) AuditService audit) {
 *         this.db = db;  // Always available
 *         this.notifications = notifications;  // May be null if unavailable
 *         this.analytics = analytics;  // May be null if unavailable
 *         this.audit = audit;  // May be null if unavailable
 *     }
 *
 *     public Order createOrder(OrderRequest request) {
 *         // Core functionality always works
 *         Order order = db.save(request);
 *
 *         // Best-effort side effects
 *         if (notifications != null) {
 *             try {
 *                 notifications.sendConfirmation(order);
 *             } catch (Exception e) {
 *                 // Log but don't fail the order
 *                 System.err.println("Notification failed: " + e.getMessage());
 *             }
 *         }
 *
 *         if (analytics != null) {
 *             analytics.track("order_created", order);
 *         }
 *
 *         return order;
 *     }
 * }
 * }</pre>
 *
 * <h3>Graceful Degradation Pattern</h3>
 * <pre>{@code
 * @RestService
 * @DependsOn(value = RecommendationService.class, optional = true)
 * public class ProductController {
 *     private final ProductService products;
 *     private final RecommendationService recommendations;
 *
 *     public ProductController(ProductService products,
 *                              @Inject(optional = true) RecommendationService recs) {
 *         this.products = products;
 *         this.recommendations = recs;
 *     }
 *
 *     public ProductPage getProductPage(String productId) {
 *         Product product = products.findById(productId);
 *
 *         List<Product> related;
 *         if (recommendations != null) {
 *             related = recommendations.getRelated(productId);
 *         } else {
 *             // Fallback: simple category-based recommendations
 *             related = products.findByCategory(product.category(), 5);
 *         }
 *
 *         return new ProductPage(product, related);
 *     }
 * }
 * }</pre>
 *
 * @see RestService
 * @see Inject
 * @see Lifecycle
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DependsOn.List.class)
@Documented
public @interface DependsOn {

    /**
     * The service class this component depends on.
     */
    Class<?> value();

    /**
     * Whether this dependency is optional.
     *
     * <p>If true, startup continues even if the dependency is unavailable.
     * The dependency will be injected as null if it cannot be resolved.
     *
     * <p>This enables microservice-style resilience where non-critical
     * services can be down without bringing down the entire application.
     *
     * @return true if the dependency is optional
     */
    boolean optional() default false;

    /**
     * Timeout in milliseconds for waiting for this dependency to be ready.
     * Only applies when {@link #optional()} is true.
     *
     * <p>Default is 30000ms (30 seconds).
     */
    long timeout() default 30000;

    /**
     * Number of retry attempts for connecting to this dependency.
     * Only applies when {@link #optional()} is true.
     *
     * <p>Default is 0 (no retries).
     */
    int retry() default 0;

    /**
     * Custom health check for determining if the dependency is available.
     * The specified method name will be called to verify the dependency is ready.
     *
     * <p>Only applies when {@link #optional()} is true.
     *
     * <pre>{@code
     * @DependsOn(value = PaymentService.class, optional = true, healthCheck = "isAvailable")
     * public class OrderService {
     *     // ...
     * }
     * }</pre>
     */
    String healthCheck() default "";

    /**
     * Container for repeatable {@link DependsOn} annotations.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        DependsOn[] value();
    }
}
