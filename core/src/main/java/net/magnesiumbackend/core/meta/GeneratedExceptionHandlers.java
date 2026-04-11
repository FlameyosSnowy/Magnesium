package net.magnesiumbackend.core.meta;

import io.github.flameyossnowy.velocis.tables.HashTable;
import io.github.flameyossnowy.velocis.tables.Table;
import net.magnesiumbackend.core.registry.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.RouteExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Compile-time-generated exception handler registry.
 *
 * <p>The annotation processor replaces this class entirely with a generated
 * version that populates {@link #GLOBAL} and {@link #LOCAL} in a static
 * initializer block. If no {@code @ExceptionHandler} annotations are found,
 * this no-op version is used as-is.
 */
public class GeneratedExceptionHandlers {
    public static final Map<Class<? extends Throwable>, RouteExceptionHandler>
        GLOBAL = new HashMap<>();

    public static final Table<RouteDefinition, Class<? extends Throwable>, RouteExceptionHandler>
        LOCAL = new HashTable<>();
}
