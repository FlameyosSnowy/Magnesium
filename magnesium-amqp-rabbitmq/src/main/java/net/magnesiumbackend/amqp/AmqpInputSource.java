package net.magnesiumbackend.amqp;

import net.magnesiumbackend.core.runtime.engine.CommandExecutor;
import net.magnesiumbackend.core.runtime.input.InputSource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Input source for AMQP message consumption.
 *
 * <p>Consumes messages from RabbitMQ queues and executes them as commands.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * RuntimeConfig config = RuntimeConfig.builder()
 *     .inputSource(new AmqpInputSource(rabbitMQService, "commands.queue"))
 *     .lifecyclePolicy(new LatchLifecyclePolicy())
 *     .build();
 * }</pre>
 */
public final class AmqpInputSource implements InputSource {

    private static final Logger logger = LoggerFactory.getLogger(AmqpInputSource.class);

    private final RabbitMQService rabbitMQService;
    private final String queueName;
    private final String consumerTag;

    private volatile boolean running = false;
    private volatile CommandExecutor executor;
    private ExecutorService consumeExecutor;

    /**
     * Creates an AMQP input source.
     *
     * @param rabbitMQService the RabbitMQ service
     * @param queueName the queue to consume from
     */
    public AmqpInputSource(@NotNull RabbitMQService rabbitMQService, @NotNull String queueName) {
        this(rabbitMQService, queueName, "magnesium-shell-consumer");
    }

    /**
     * Creates an AMQP input source with custom consumer tag.
     *
     * @param rabbitMQService the RabbitMQ service
     * @param queueName the queue to consume from
     * @param consumerTag the consumer identifier
     */
    public AmqpInputSource(
        @NotNull RabbitMQService rabbitMQService,
        @NotNull String queueName,
        @NotNull String consumerTag
    ) {
        this.rabbitMQService = rabbitMQService;
        this.queueName = queueName;
        this.consumerTag = consumerTag;
    }

    @Override
    public void start(@NotNull CommandExecutor executor) {
        this.executor = executor;
        this.running = true;

        // Start consumer in a virtual thread
        this.consumeExecutor = Executors.newVirtualThreadPerTaskExecutor();
        consumeExecutor.submit(this::runConsumer);

        logger.info("AmqpInputSource started consuming from queue: {}", queueName);
    }

    @Override
    public void stop() {
        running = false;

        try {
            rabbitMQService.unsubscribe(queueName, consumerTag);
        } catch (Exception e) {
            logger.warn("Error unsubscribing from queue: {}", e.getMessage());
        }

        if (consumeExecutor != null) {
            consumeExecutor.shutdownNow();
        }

        logger.info("AmqpInputSource stopped");
    }

    private void runConsumer() {
        Consumer<String> messageHandler = message -> {
            if (!running) {
                return;
            }

            try {
                logger.debug("Processing AMQP message: {}", message);
                int exitCode = executor.execute(message);

                if (exitCode != 0) {
                    logger.warn("Command failed with exit code: {}", exitCode);
                }
            } catch (Exception e) {
                logger.warn("Error processing message: {}", e.getMessage());
            }
        };

        try {
            rabbitMQService.subscribe(queueName, consumerTag, messageHandler);
        } catch (Exception e) {
            logger.error("Failed to start AMQP consumer: {}", e.getMessage());
            throw new RuntimeException("AMQP consumer failed", e);
        }
    }

    @Override
    public @NotNull String name() {
        return "amqp-" + queueName;
    }
}
