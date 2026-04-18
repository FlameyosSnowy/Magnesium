package net.magnesiumbackend.core.test.junit;

import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.test.TestServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * JUnit 5 extension for Magnesium integration tests.
 *
 * <p>Automatically starts a test server before tests and shuts it down after.
 * The application class is determined from the test class or method annotations.
 *
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(MagnesiumExtension.class)
 * @MagnesiumTest(MyApplication.class)
 * class MyIntegrationTest {
 *
 *     @Test
 *     void testEndpoint(TestClient client) {
 *         HttpResponse<String> response = client.get("/health");
 *         assertEquals(200, response.statusCode());
 *     }
 * }
 * }</pre>
 *
 * <p>With custom configuration:
 * <pre>{@code
 * @ExtendWith(MagnesiumExtension.class)
 * @MagnesiumTest(value = MyApplication.class, port = 9090)
 * class MyIntegrationTest {
 *     // Tests run against port 9090
 * }
 * }</pre>
 */
public class MagnesiumExtension implements BeforeAllCallback, AfterAllCallback,
    BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(MagnesiumExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        MagnesiumTest annotation = findAnnotation(context);
        if (annotation == null) {
            return;
        }

        Class<? extends Application> appClass = annotation.value();
        int port = annotation.port();

        try {
            Application application = createApplication(appClass);
            TestServer server = new TestServer(application, port);

            // Store in context for access during tests
            getStore(context).put("server", server);
            getStore(context).put("client", new TestClient(server));
        } catch (Exception e) {
            throw new ExtensionConfigurationException(
                "Failed to start Magnesium test server", e);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        TestServer server = getStore(context).get("server", TestServer.class);
        if (server != null) {
            server.close();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        // Per-test setup if needed
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // Per-test cleanup if needed
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                    ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type.equals(TestClient.class)
            || type.equals(TestServer.class)
            || type.equals(MagnesiumRuntime.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();

        if (type.equals(TestClient.class)) {
            return getStore(extensionContext).get("client");
        }
        if (type.equals(TestServer.class)) {
            return getStore(extensionContext).get("server");
        }
        if (type.equals(MagnesiumRuntime.class)) {
            TestServer server = getStore(extensionContext).get("server", TestServer.class);
            return server != null ? server.runtime() : null;
        }

        throw new ParameterResolutionException(
            "Unsupported parameter type: " + type.getName());
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }

    private MagnesiumTest findAnnotation(ExtensionContext context) {
        // Check test class
        MagnesiumTest annotation = context.getRequiredTestClass()
            .getAnnotation(MagnesiumTest.class);

        if (annotation != null) {
            return annotation;
        }

        // Check enclosing classes
        Class<?> clazz = context.getRequiredTestClass();
        while (clazz.getEnclosingClass() != null) {
            clazz = clazz.getEnclosingClass();
            annotation = clazz.getAnnotation(MagnesiumTest.class);
            if (annotation != null) {
                return annotation;
            }
        }

        return null;
    }

    private @NotNull Application createApplication(@NotNull Class<? extends Application> appClass)
        throws Exception {
        try {
            Constructor<? extends Application> constructor = appClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new ExtensionConfigurationException(
                "Application class " + appClass.getName() +
                " must have a no-args constructor", e);
        }
    }
}
