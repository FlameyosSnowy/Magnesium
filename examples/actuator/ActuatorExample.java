package examples.actuator;

import net.magnesiumbackend.actuator.ActuatorConfig;
import net.magnesiumbackend.actuator.ActuatorController;
import net.magnesiumbackend.actuator.health.CompositeHealthIndicator;
import net.magnesiumbackend.actuator.health.DiskSpaceHealthIndicator;
import net.magnesiumbackend.actuator.health.Health;
import net.magnesiumbackend.actuator.health.HealthIndicator;
import net.magnesiumbackend.actuator.health.JvmHealthIndicator;
import net.magnesiumbackend.actuator.metrics.MetricsRegistry;
import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.http.response.Response;

/**
 * Example application demonstrating actuator endpoints.
 *
 * <p>Run with: <code>mvn exec:java -Dexec.mainClass=examples.actuator.ActuatorExample</code></p>
 *
 * <p>Endpoints available:</p>
 * <ul>
 *   <li><code>GET http://localhost:8080/actuator/health</code> - Full health check</li>
 *   <li><code>GET http://localhost:8080/actuator/health/liveness</code> - Kubernetes liveness</li>
 *   <li><code>GET http://localhost:8080/actuator/health/readiness</code> - Kubernetes readiness</li>
 *   <li><code>GET http://localhost:8080/actuator/metrics</code> - Application metrics</li>
 * </ul>
 */
public class ActuatorExample implements Application {

    @Override
    public void configure(MagnesiumRuntime runtime) {
        // Configure actuator (optional - defaults work too)
        runtime.services(s -> s
            .register(ActuatorConfig.class, ctx -> ActuatorConfig.builder()
                .basePath("/actuator")
                .enableJvmHealth(true)
                .enableDiskHealth(true)
                .build()));

        // Add custom health indicator
        runtime.services(s -> {
            s.register(CompositeHealthIndicator.class, ctx -> {
                CompositeHealthIndicator composite = new CompositeHealthIndicator();

                // Add built-in indicators
                composite.add(new JvmHealthIndicator());
                composite.add(new DiskSpaceHealthIndicator());

                // Add custom indicator
                composite.add(new CustomHealthIndicator());

                return composite;
            });
        });

        // Add custom metrics
        runtime.services(s -> {
            s.register(MetricsRegistry.class, ctx -> {
                MetricsRegistry metrics = new MetricsRegistry("myapp.");

                // Counter
                MetricsRegistry.Counter requests = metrics.counter("http.requests");
                requests.increment();

                // Gauge
                metrics.gauge("active.sessions", () -> 42L);

                // Timer
                MetricsRegistry.Timer dbTimer = metrics.timer("db.query");
                dbTimer.record(() -> {
                    // Simulate database query
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

                return metrics;
            });
        });

        // Regular application routes
        runtime.router()
            .get("/api/hello", ctx -> {
                // Increment counter on each request
                MetricsRegistry metrics = runtime.serviceRegistry().get(MetricsRegistry.class);
                metrics.counter("http.requests").increment();

                return Response.ok("Hello, World!");
            })
            .commit();
    }

    public static void main(String[] args) {
        MagnesiumApplication.builder()
            .mainClass(ActuatorExample.class)
            .run(args);
    }

    /**
     * Custom health indicator example.
     */
    static class CustomHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            // Simulate a custom check
            boolean healthy = Math.random() > 0.1; // 90% healthy

            if (healthy) {
                return Health.up()
                    .withDetail("service", "custom-service")
                    .withDetail("version", "1.0.0")
                    .build();
            } else {
                return Health.down()
                    .withDetail("service", "custom-service")
                    .withDetail("error", "Random failure simulation")
                    .build();
            }
        }

        @Override
        public String name() {
            return "customService";
        }
    }
}
