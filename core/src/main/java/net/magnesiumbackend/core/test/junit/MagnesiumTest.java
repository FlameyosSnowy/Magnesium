package net.magnesiumbackend.core.test.junit;

import net.magnesiumbackend.core.Application;

import java.lang.annotation.*;

/**
 * Marks a test class for Magnesium integration testing.
 *
 * <p>Used with {@link MagnesiumExtension} to configure the test server.
 *
 * <pre>{@code
 * @ExtendWith(MagnesiumExtension.class)
 * @MagnesiumTest(MyApplication.class)
 * class MyIntegrationTest {
 *     // Tests run with MyApplication started on an available port
 * }
 * }</pre>
 *
 * <p>With custom port:
 * <pre>{@code
 * @MagnesiumTest(value = MyApplication.class, port = 9090)
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MagnesiumTest {

    /**
     * The application class to test.
     */
    Class<? extends Application> value();

    /**
     * The port to bind to. 0 means any available port.
     */
    int port() default 0;

    /**
     * Whether to call the application's start() lifecycle hook.
     */
    boolean callStart() default true;

    /**
     * Whether to call the application's stop() lifecycle hook.
     */
    boolean callStop() default true;
}
