package net.magnesiumbackend.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import net.magnesiumbackend.amqp.annotations.DeadLetterQueue;
import net.magnesiumbackend.amqp.annotations.QueueArgument;
import net.magnesiumbackend.amqp.annotations.QueueListener;
import net.magnesiumbackend.core.json.JsonProvider;
import net.magnesiumbackend.core.lifecycle.Startable;
import net.magnesiumbackend.core.lifecycle.Stoppable;
import net.magnesiumbackend.core.services.ServiceContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registry for queue listeners with dependency graph integration.
 *
 * <p>Manages listener lifecycle, retry logic, dead letter routing, and
 * concurrent message processing.</p>
 */
public class QueueListenerRegistry implements Startable, Stoppable {

    private final RabbitMQService rabbitMQService;
    private final ServiceContext serviceContext;
    private final JsonProvider jsonProvider;
    private final Map<String, ListenerConfig> listeners = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();
    private final Map<String, String> consumerTags = new ConcurrentHashMap<>();

    public QueueListenerRegistry(
        @NotNull RabbitMQService rabbitMQService,
        @NotNull ServiceContext serviceContext,
        @NotNull JsonProvider jsonProvider
    ) {
        this.rabbitMQService = rabbitMQService;
        this.serviceContext = serviceContext;
        this.jsonProvider = jsonProvider;
    }

    /**
     * Registers a queue listener with compile-time generated invoker.
     *
     * <p>Uses code-generated invoker to eliminate runtime reflection.</p>
     *
     * @param listener annotation configuration
     * @param deadLetter DLQ configuration (may be null)
     * @param invoker the compile-time generated invoker
     */
    public void registerListener(
        @NotNull QueueListener listener,
        DeadLetterQueue deadLetter,
        @NotNull QueueListenerInvoker invoker
    ) {
        String queue = listener.queue();
        Object instance = invoker.createInstance(serviceContext);
        listeners.put(queue, new ListenerConfig(listener, deadLetter, instance, invoker));
    }

    @Override
    public void start() throws Exception {
        // Load and execute generated RabbitMQ wiring classes
        ServiceLoader<RabbitMQWiring> loader = ServiceLoader.load(
            RabbitMQWiring.class,
            this.getClass().getClassLoader()
        );

        for (RabbitMQWiring wiring : loader) {
            wiring.wire(rabbitMQService, this, jsonProvider);
        }

        // Start all registered listeners
        for (Map.Entry<String, ListenerConfig> entry : listeners.entrySet()) {
            startListener(entry.getKey(), entry.getValue());
        }
    }

    private void startListener(String queue, ListenerConfig config) throws IOException {
        QueueListener listener = config.listener();
        DeadLetterQueue dlq = config.deadLetter();

        // Create thread pool for concurrent processing
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        executors.put(queue, executor);

        Channel channel = rabbitMQService.getChannel("listener-" + queue);

        // Declare queue with arguments
        Map<String, Object> args = new HashMap<>();
        for (QueueArgument arg : listener.arguments()) {
            args.put(arg.name(), parseArgumentValue(arg.value()));
        }

        // Setup DLQ if configured
        if (dlq != null) {
            args.put("x-dead-letter-exchange", dlq.exchange());
            if (!dlq.routingKey().isEmpty()) {
                args.put("x-dead-letter-routing-key", dlq.routingKey());
            }

            // Declare DLX and DLQ
            rabbitMQService.declareExchange(new net.magnesiumbackend.amqp.annotations.Exchange() {
                @Override public String name() { return dlq.exchange(); }
                @Override public net.magnesiumbackend.amqp.annotations.ExchangeType type() { return net.magnesiumbackend.amqp.annotations.ExchangeType.DIRECT; }
                @Override public boolean durable() { return dlq.durable(); }
                @Override public boolean autoDelete() { return false; }
                @Override public boolean internal() { return false; }
                @Override public String alternateExchange() { return ""; }
                @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return net.magnesiumbackend.amqp.annotations.Exchange.class; }
            });

            Map<String, Object> dlqArgs = new HashMap<>();
            if (dlq.messageTtl() > 0) {
                dlqArgs.put("x-message-ttl", dlq.messageTtl());
            }
            if (dlq.maxLength() > 0) {
                dlqArgs.put("x-max-length", dlq.maxLength());
            }

            rabbitMQService.declareQueue(dlq.queue(), dlq.durable(), false, false, dlqArgs);
            rabbitMQService.bindQueue(dlq.queue(), dlq.exchange(), dlq.queue());
        }

        // Declare main queue
        rabbitMQService.declareQueue(queue, listener.durable(), listener.exclusive(), listener.autoDelete(), args);

        // Start consumers
        AtomicInteger activeConsumers = new AtomicInteger(0);

        for (int i = 0; i < listener.concurrency(); i++) {
            Channel consumerChannel = rabbitMQService.getChannel(queue + "-consumer-" + i);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                executor.submit(() -> {
                    processMessage(config, delivery, consumerChannel, activeConsumers);
                });
            };

            String tag = consumerChannel.basicConsume(queue, listener.autoAck(), deliverCallback, consumerTag -> {});
            consumerTags.put(queue + "-" + i, tag);
        }
    }

    private void processMessage(ListenerConfig config, Delivery delivery, Channel channel, AtomicInteger retryCount) {
        QueueListener listener = config.listener();
        Object target = config.target();
        QueueListenerInvoker invoker = config.invoker();

        int retries = 0;
        int maxRetries = listener.maxRetries();

        while (retries <= maxRetries) {
            try {
                if (config.usesInvoker()) {
                    // Use compile-time generated invoker (no reflection)
                    invoker.invoke(target, delivery, channel, jsonProvider);
                } else {
                    // Legacy reflection-based invocation (deprecated path)
                    throw new IllegalStateException(
                        "Legacy reflection-based listeners not supported. " +
                        "Please regenerate code with the annotation processor."
                    );
                }

                if (!listener.autoAck()) {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
                return; // Success
            } catch (Exception e) {
                retries++;

                if (retries > maxRetries) {
                    // Max retries exceeded - reject and route to DLQ
                    try {
                        if (!listener.autoAck()) {
                            channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
                        }
                    } catch (IOException ioException) {
                        // Log error
                    }
                    return;
                }

                // Wait before retry
                try {
                    Thread.sleep(listener.retryDelay());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @Override
    public void stop() throws Exception {
        // Cancel consumers
        for (Map.Entry<String, String> entry : consumerTags.entrySet()) {
            try {
                String[] parts = entry.getKey().split("-", 2);
                String queue = parts[0];
                Channel channel = rabbitMQService.getChannel(queue + "-consumer-" + parts[1]);
                channel.basicCancel(entry.getValue());
            } catch (Exception e) {
                // Log error
            }
        }

        // Shutdown executors
        for (ExecutorService executor : executors.values()) {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private Object parseArgumentValue(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e2) {
                if (value.equalsIgnoreCase("true")) return true;
                if (value.equalsIgnoreCase("false")) return false;
                return value;
            }
        }
    }

    private record ListenerConfig(
        QueueListener listener,
        DeadLetterQueue deadLetter,
        Object target,
        QueueListenerInvoker invoker
    ) {
        boolean usesInvoker() {
            return invoker != null;
        }
    }
}
