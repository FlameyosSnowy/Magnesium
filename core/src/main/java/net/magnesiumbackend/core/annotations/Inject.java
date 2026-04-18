package net.magnesiumbackend.core.annotations;

import java.lang.annotation.*;

/**
 * Marks a constructor parameter for dependency injection.
 *
 * <p>Used within {@link RestService}-annotated classes to declare dependencies
 * that should be automatically resolved by the Magnesium container.
 *
 * <p><strong>Constructor injection is the only supported injection mode.</strong>
 * All dependencies must be declared as constructor parameters, promoting
 * immutable service design and making dependencies explicit.
 *
 * <h3>Constructor Injection</h3>
 * <pre>{@code
 * @RestService
 * public class OrderService {
 *     private final UserService users;
 *     private final PaymentService payments;
 *
 *     // All parameters auto-injected
 *     public OrderService(UserService users, PaymentService payments) {
 *         this.users = users;
 *         this.payments = payments;
 *     }
 * }
 * }</pre>
 *
 * <h3>Qualified Injection (by Name)</h3>
 * <pre>{@code
 * @RestService
 * public class DataPipeline {
 *     private final DatabaseService primaryDb;
 *     private final DatabaseService replicaDb;
 *     private final CacheService cache;
 *
 *     public DataPipeline(
 *         @Inject("primary") DatabaseService primaryDb,
 *         @Inject("replica") DatabaseService replicaDb,
 *         @Inject("cache") CacheService cache) {
 *         this.primaryDb = primaryDb;
 *         this.replicaDb = replicaDb;
 *         this.cache = cache;
 *     }
 * }
 * }</pre>
 *
 * <h3>Optional Dependencies</h3>
 * <pre>{@code
 * @RestService
 * @DependsOn(value = AuditService.class, optional = true)
 * public class ProductService {
 *     private final DatabaseService db;
 *     private final AuditService audit;
 *
 *     public ProductService(
 *         DatabaseService db,
 *         @Inject(optional = true) AuditService audit) {
 *         this.db = db;
 *         this.audit = audit;  // null if AuditService unavailable
 *     }
 *
 *     public Product updateProduct(String id, ProductUpdate update) {
 *         Product product = db.update(id, update);
 *
 *         if (audit != null) {
 *             audit.log("PRODUCT_UPDATED", product);
 *         }
 *
 *         return product;
 *     }
 * }
 * }</pre>
 *
 * @see RestService
 * @see DependsOn
 */
@Target({ElementType.PARAMETER, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Inject {

    /**
     * The name/qualifier for this dependency.
     *
     * <p>Used to disambiguate when multiple services implement the same interface:
     * <pre>{@code
     * public DataPipeline(@Inject("primary") DatabaseService db) { ... }
     * }</pre>
     *
     * <p>If not specified, the dependency is resolved by type.
     */
    String value() default "";

    /**
     * Whether this dependency is optional.
     *
     * <p>If true and the dependency cannot be resolved, null is injected
     * instead of throwing an error.
     *
     * <p>This should be combined with {@link DependsOn#optional()} at the
     * class level for proper lifecycle management.
     *
     * <pre>{@code
     * @RestService
     * @DependsOn(value = MetricsService.class, optional = true)
     * public class MyService {
     *     public MyService(@Inject(optional = true) MetricsService metrics) {
     *         // metrics may be null
     *     }
     * }
     * }</pre>
     */
    boolean optional() default false;

    /**
     * Custom factory method for creating this dependency.
     *
     * <p>Specifies a static factory method on the target service class
     * to use instead of the constructor.
     *
     * <pre>{@code
     * public MyService(@Inject(factory = "createTestInstance") CustomService service) { ... }
     * }</pre>
     */
    String factory() default "";
}
