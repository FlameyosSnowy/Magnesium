package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.base.MagnesiumController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an exception handler for a controller or globally.
 *
 * <p>Annotated methods are registered at compile time to handle exceptions
 * thrown during request processing. Handlers can be controller-specific or
 * global based on the {@code controllerType} attribute.</p>
 *
 * <p>The annotated method must accept two parameters:
 * <ol>
 *   <li>The exception type to handle (or a superclass)</li>
 *   <li>{@link net.magnesiumbackend.core.route.RequestContext} for request information</li>
 * </ol>
 * </p>
 *
 * <p>The return type should be a {@link net.magnesiumbackend.core.http.response.ResponseEntity}
 * or a type convertible to a response.</p>
 *
 * <h3>Global Exception Handler</h3>
 * <pre>{@code
 * @ExceptionHandler
 * public class GlobalExceptionHandlers {
 *     public ResponseEntity<ErrorResponse> handleValidationException(
 *             ValidationException ex,
 *             RequestContext ctx) {
 *         return ResponseEntity.status(400)
 *             .body(new ErrorResponse(ex.getMessage()));
 *     }
 * }
 * }</pre>
 *
 * <h3>Controller-Specific Handler</h3>
 * <pre>{@code
 * @RestController
 * public class OrderController {
 *     @ExceptionHandler(controllerType = OrderController.class)
 *     public ResponseEntity<ErrorResponse> handleNotFound(
 *             OrderNotFoundException ex,
 *             RequestContext ctx) {
 *         return ResponseEntity.status(404)
 *             .body(new ErrorResponse("Order not found: " + ex.getOrderId()));
 *     }
 * }
 * }</pre>
 *
 * @see RestController
 * @see net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry
 * @see net.magnesiumbackend.core.route.RequestContext
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ExceptionHandler {
    /**
     * Specifies the controller type this handler applies to.
     *
     * <p>Defaults to {@code MagnesiumController.class}, which indicates a global
     * handler that applies to all controllers. Set to a specific controller class
     * to make the handler apply only to that controller's routes.</p>
     *
     * @return the controller class this handler is bound to
     */
    Class<? extends MagnesiumController> controllerType() default MagnesiumController.class;
}
