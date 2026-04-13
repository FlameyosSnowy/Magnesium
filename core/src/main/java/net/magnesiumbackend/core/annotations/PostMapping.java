package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps HTTP POST requests to a handler method.
 *
 * <p>The annotated method will be invoked when an HTTP POST request matches
 * the specified path pattern. POST requests are typically used to create
 * new resources or submit data that causes a change in server state.</p>
 *
 * <p>Path patterns may include variables in curly braces, e.g., {@code "/orders/{orderId}/items"}.
 * Method parameters can be bound to these variables using {@link PathParam}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class OrderController {
 *     @PostMapping(path = "/orders")
 *     public ResponseEntity<Order> createOrder(@Body OrderRequest request) {
 *         Order created = orderService.create(request);
 *         return ResponseEntity.status(201).body(created);
 *     }
 * }
 * }</pre>
 *
 * @see RestController
 * @see PathParam
 * @see GetMapping
 * @see PutMapping
 * @see DeleteMapping
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface PostMapping {
    /**
     * The path pattern for this route.
     *
     * <p>May include path variables in curly braces, e.g., {@code "/orders/{id}"}.
     * Exact paths are also valid.</p>
     *
     * @return the route path pattern
     */
    String path();
}