package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called during component shutdown/cleanup.
 *
 * <p>Methods annotated with @PreDestroy are invoked by the
 * {@link net.magnesiumbackend.core.lifecycle.LifecycleRegistry} when the application
 * is shutting down, allowing services to perform cleanup operations such as:
 * <ul>
 *   <li>Closing database connections</li>
 *   <li>Releasing external resources</li>
 *   <li>Flushing caches or buffers</li>
 *   <li>Stopping background threads</li>
 *   <li>Notifying dependent services</li>
 * </ul>
 *
 * <p>The method should be:</p>
 * <ul>
 *   <li>Non-private (package-private, protected, or public)</li>
 *   <li>Void return type</li>
 *   <li>No parameters</li>
 *   <li>Non-blocking and quick to execute</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @RestService
 * public class DatabaseService {
 *     private ConnectionPool connectionPool;
 *
 *     @OnInitialize
 *     void connect() {
 *         this.connectionPool = ConnectionPool.create();
 *     }
 *
 *     @PreDestroy
 *     void disconnect() {
 *         if (connectionPool != null) {
 *             connectionPool.close();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Shutdown Order</h3>
 * <p>PreDestroy methods are called in reverse initialization order. Services
 * initialized last are destroyed first, respecting the dependency graph.</p>
 *
 * @see OnInitialize
 * @see RestService
 * @see Lifecycle
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface PreDestroy {
}
