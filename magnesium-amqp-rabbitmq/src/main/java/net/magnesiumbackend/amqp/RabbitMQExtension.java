package net.magnesiumbackend.amqp;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.extensions.MagnesiumExtension;
import org.jetbrains.annotations.NotNull;

/**
 * RabbitMQ extension for Magnesium framework.
 *
 * <p>Integrates RabbitMQ messaging with Magnesium's lifecycle and
 * dependency injection system using compile-time generated wiring.</p>
 *
 * <p>Generated {@link RabbitMQWiring} classes are automatically discovered
 * via {@link ServiceLoader} and executed to set up listeners and publishers.</p>
 */
public class RabbitMQExtension implements MagnesiumExtension {

    @Override
    public void configure(@NotNull MagnesiumRuntime runtime) {
        runtime.services(services -> {
            services.register(RabbitMQConfiguration.class, ctx ->
                ctx.configurationManager().get(RabbitMQConfiguration.class)
            );

            services.register(RabbitMQService.class, ctx -> {
                RabbitMQConfiguration config = ctx.get(RabbitMQConfiguration.class);
                return new RabbitMQService(config);
            });

            services.register(QueueListenerRegistry.class, ctx ->
                new QueueListenerRegistry(
                    ctx.get(RabbitMQService.class),
                    ctx,
                    ctx.jsonProvider()
                )
            );
        });

        // RabbitMQWiring classes are loaded automatically by QueueListenerRegistry
        // via ServiceLoader when it starts (implements Startable)
    }

    @Override
    public @NotNull String name() {
        return "rabbitmq";
    }
}
