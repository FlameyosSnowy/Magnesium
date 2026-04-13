package net.magnesiumbackend.core.base;

/**
 * Marker interface for all Magnesium REST controllers.
 *
 * <p>Classes annotated with {@link net.magnesiumbackend.core.annotations.RestController}
 * implicitly implement this interface through code generation. The marker interface
 * enables identification of controller types at runtime and serves as the default
 * value for {@link net.magnesiumbackend.core.annotations.ExceptionHandler#controllerType()}.</p>
 *
 * <p>Application controllers should not directly implement this interface; instead,
 * use the {@code @RestController} annotation which triggers the necessary code generation.</p>
 *
 * @see net.magnesiumbackend.core.annotations.RestController
 * @see net.magnesiumbackend.core.annotations.ExceptionHandler
 */
public interface MagnesiumController {
}
