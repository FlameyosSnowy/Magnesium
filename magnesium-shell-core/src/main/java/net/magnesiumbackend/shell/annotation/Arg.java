package net.magnesiumbackend.shell.annotation;

import java.lang.annotation.*;

/**
 * Marks a field as a command argument.
 *
 * <p>Example:</p>
 * <pre>{@code
 * @Command(name = "user:create")
 * class CreateUserCommand {
 *     @Arg(required = true, description = "User email")
 *     String email;
 *
 *     @Arg(defaultValue = "USER")
 *     String role;
 *
 *     @Arg(flag = true)
 *     boolean admin;
 * }
 * }</pre>
 *
 * @see Command
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Documented
public @interface Arg {

    /**
     * Argument name (defaults to field name).
     *
     * @return the argument name
     */
    String value() default "";

    /**
     * Whether this argument is required.
     *
     * @return true if required
     */
    boolean required() default false;

    /**
     * Default value if not provided.
     *
     * @return default value as string
     */
    String defaultValue() default "";

    /**
     * Description for help text.
     *
     * @return the description
     */
    String description() default "";

    /**
     * Whether this is a positional argument (not a named flag).
     *
     * @return true if positional
     */
    boolean positional() default false;

    /**
     * Whether this is a boolean flag (e.g., --verbose).
     *
     * @return true if a flag
     */
    boolean flag() default false;
}
