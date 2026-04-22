package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a compile-time return resolver for a specific type.
 *
 * <p>The annotation processor discovers these at compile time and wires
 * them into the generated dispatcher. The resolver must have a
 * public no-arg constructor.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * @ReturnResolverType(Result.class)
 * public class ResultResolver implements ReturnResolver<Result<?>> {
 *     @Override
 *     public CompletableFuture<ResponseEntity<?>> resolve(Result<?> value, ResolverContext ctx) {
 *         if (value.isError()) {
 *             throw new MagnesiumExecutionException(value.error());
 *         }
 *         return ctx.resolveNext(value.getValue());
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ReturnResolverType {

    /**
     * The type this resolver handles.
     *
     * @return the resolved type class
     */
    Class<?> value();
}
