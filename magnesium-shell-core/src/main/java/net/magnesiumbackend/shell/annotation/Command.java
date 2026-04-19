package net.magnesiumbackend.shell.annotation;

import net.magnesiumbackend.shell.ir.ExecutionMode;

import java.lang.annotation.*;

/**
 * Marks a class as a CLI command.
 *
 * <p>Example:</p>
 * <pre>{@code
 * @Command(name = "user:create", description = "Create a new user")
 * class CreateUserCommand {
 *     @Arg(required = true) String email;
 *     @Arg(defaultValue = "USER") String role;
 *
 *     void run() {
 *         // implementation
 *     }
 * }
 * }</pre>
 *
 * @see Arg
 * @see ExecutionMode
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Documented
public @interface Command {

    /**
     * Command name (e.g., "user:create").
     *
     * @return the command name
     */
    String name();

    /**
     * Command description for help text.
     *
     * @return the description
     */
    String description() default "";

    /**
     * Execution mode.
     *
     * @return the execution mode
     */
    ExecutionMode mode() default ExecutionMode.LOCAL;

    /**
     * AMQP binding (required if mode is AMQP).
     *
     * @return the AMQP exchange/routing key
     */
    String amqpBinding() default "";

    /**
     * Command aliases.
     *
     * @return array of aliases
     */
    String[] aliases() default {};
}
