package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.lifecycle.LifecycleRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called during component initialization.
 *
 * <p>Methods annotated with @OnInitialize are invoked by the
 * {@link LifecycleRegistry} when the component's lifecycle stage is reached.
 * The method should be non-blocking for async components.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @Lifecycle(stage = LifecycleStage.INIT)
 * public class DatabaseService {
 *     @OnInitialize
 *     void connect() {
 *         // Initialize database connection
 *     }
 * }
 * }</pre>
 *
 * @see Lifecycle
 * @see LifecycleRegistry
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OnInitialize {
}
