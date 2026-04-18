package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.annotations.enums.LifecycleStage;

import java.lang.annotation.*;

/**
 * Marks a class as a Magnesium-managed service with auto-wiring support.
 *
 * <p>Services annotated with {@code @RestService} are automatically:
 * <ul>
 *   <li>Registered in the {@link net.magnesiumbackend.core.services.ServiceRegistry}</li>
 *   <li>Instantiated with constructor dependency injection</li>
 *   <li>Initialized according to their lifecycle stage</li>
 *   <li>Made available for injection into controllers and other services</li>
 * </ul>
 *
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * @RestService
 * public class UserService {
 *     private final DatabaseService db;
 *
 *     // Constructor injection - dependencies auto-resolved
 *     public UserService(DatabaseService db) {
 *         this.db = db;
 *     }
 *
 *     public User findById(String id) {
 *         return db.query("SELECT * FROM users WHERE id = ?", id);
 *     }
 * }
 * }</pre>
 *
 * <h3>With Lifecycle</h3>
 * <pre>{@code
 * @RestService
 * @Lifecycle(stage = LifecycleStage.INIT)
 * @DependsOn(ConfigService.class)
 * public class DatabaseService {
 *     private final ConfigService config;
 *     private ConnectionPool pool;
 *
 *     public DatabaseService(ConfigService config) {
 *         this.config = config;
 *     }
 *
 *     @OnInitialize
 *     void connect() {
 *         this.pool = ConnectionPool.create(config.getDbUrl());
 *     }
 *
 *     @PreDestroy
 *     void disconnect() {
 *         if (pool != null) {
 *             pool.close();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Microservice-Style with Optional Dependencies</h3>
 * <pre>{@code
 * @RestService
 * @Lifecycle(stage = LifecycleStage.POST_INIT)
 * @DependsOn(value = NotificationService.class, optional = true)
 * @DependsOn(value = AnalyticsService.class, optional = true)
 * public class OrderProcessor {
 *     private final DatabaseService db;
 *     private final NotificationService notifications;
 *     private final AnalyticsService analytics;
 *
 *     public OrderProcessor(
 *         DatabaseService db,
 *         @Inject(optional = true) NotificationService notifications,
 *         @Inject(optional = true) AnalyticsService analytics) {
 *         this.db = db;
 *         this.notifications = notifications;
 *         this.analytics = analytics;
 *     }
 *
 *     public void process(Order order) {
 *         db.save(order);
 *
 *         // These may be null if services are unavailable (crashed/degraded)
 *         if (notifications != null) {
 *             notifications.sendOrderConfirmation(order);
 *         }
 *         if (analytics != null) {
 *             analytics.trackOrder(order);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @see Inject
 * @see DependsOn
 * @see Lifecycle
 * @see OnInitialize
 * @see PreDestroy
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestService {

    /**
     * The interface type this service should be registered as.
     * If not specified, the class itself is used as the registration type.
     *
     * <p>Useful for registering implementations under their interface:
     * <pre>{@code
     * @RestService(as = UserRepository.class)
     * public class JdbcUserRepository implements UserRepository { ... }
     * }</pre>
     */
    Class<?> as() default void.class; // void.class means "use the class itself"

    /**
     * Whether this service should be a singleton (default) or prototype.
     * Prototype services are created fresh for each injection point.
     */
    Scope scope() default Scope.SINGLETON;

    /**
     * Whether this service is required for application startup.
     * If false, startup continues even if this service fails to initialize.
     */
    boolean required() default true;

    /**
     * Whether initialization should be async. Shorthand for {@code @Lifecycle(async = true)}.
     */
    boolean async() default false;

    /**
     * Service instantiation scope.
     */
    enum Scope {
        /**
         * Single instance shared across the application (default).
         */
        SINGLETON,

        /**
         * New instance created for each injection point.
         */
        PROTOTYPE,

        /**
         * New instance per request (HTTP request scope).
         */
        REQUEST
    }
}
