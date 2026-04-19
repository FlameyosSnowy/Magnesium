package net.magnesiumbackend.examples.amqp;

import net.magnesiumbackend.amqp.RabbitMQExtension;
import net.magnesiumbackend.amqp.annotations.*;
import net.magnesiumbackend.amqp.MessagePublisher;
import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.annotations.RestController;
import net.magnesiumbackend.core.annotations.GetMapping;
import net.magnesiumbackend.core.annotations.PostMapping;
import net.magnesiumbackend.core.http.request.Request;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.annotations.service.Service;

/**
 * Example demonstrating RabbitMQ integration with Magnesium.
 *
 * <p>Features shown:</p>
 * <ul>
 *   <li>Queue listeners with retry and DLQ</li>
 *   <li>Message publishing</li>
 *   <li>Exchange declarations</li>
 *   <li>Routing key patterns</li>
 *   <li>Dead letter queues</li>
 * </ul>
 */
public class RabbitMQExample extends Application {

    // Message types
    public record OrderEvent(String orderId, String type, double amount) {}
    public record PaymentEvent(String paymentId, String orderId, String status) {}
    public record NotificationEvent(String userId, String message, String channel) {}

    /**
     * Order service with RabbitMQ publishing.
     */
    @Service
    @Exchange(name = "orders", type = ExchangeType.TOPIC, durable = true)
    public static class OrderService {

        @RabbitPublisher(exchange = "orders", routingKey = "order.created")
        private MessagePublisher<OrderEvent> orderPublisher;

        @RabbitPublisher(exchange = "orders", routingKey = "order.cancelled")
        private MessagePublisher<OrderEvent> cancelPublisher;

        public void createOrder(String orderId, double amount) {
            OrderEvent event = new OrderEvent(orderId, "created", amount);
            orderPublisher.publish(event);
        }

        public void cancelOrder(String orderId) {
            OrderEvent event = new OrderEvent(orderId, "cancelled", 0);
            cancelPublisher.publish(event);
        }
    }

    /**
     * Payment service with RabbitMQ publishing.
     */
    @Service
    @Exchange(name = "payments", type = ExchangeType.TOPIC)
    public static class PaymentService {

        @RabbitPublisher(exchange = "payments")
        private MessagePublisher<PaymentEvent> paymentPublisher;

        public void processPayment(String paymentId, String orderId) {
            PaymentEvent event = new PaymentEvent(paymentId, orderId, "processing");
            paymentPublisher.publish(event, "payment.processing");
        }
    }

    /**
     * Order event listener with DLQ and retry.
     */
    @Service
    public static class OrderEventListener {

        @QueueListener(
            queue = "orders.processing",
            concurrency = 5,
            maxRetries = 3,
            retryDelay = 2000,
            durable = true
        )
        @Exchange(name = "orders", type = ExchangeType.TOPIC, durable = true)
        @Binding(exchange = "orders", routingKey = "order.created")
        @Binding(exchange = "orders", routingKey = "order.updated")
        @DeadLetterQueue(
            queue = "orders.processing.dlq",
            exchange = "orders.dlx",
            durable = true,
            messageTtl = 86400000 // 24 hours
        )
        public void handleOrderEvent(
            OrderEvent event,
            @RoutingKey String routingKey,
            com.rabbitmq.client.AMQP.BasicProperties properties
        ) {
            System.out.println("Processing order: " + event.orderId() +
                " type: " + event.type() +
                " routingKey: " + routingKey);

            // Process the order
            if ("created".equals(event.type())) {
                // Handle new order
            } else if ("cancelled".equals(event.type())) {
                // Handle cancellation
            }
        }

        @QueueListener(
            queue = "orders.cancelled",
            concurrency = 2
        )
        @Binding(exchange = "orders", routingKey = "order.cancelled")
        public void handleCancelledOrder(OrderEvent event) {
            System.out.println("Order cancelled: " + event.orderId());
        }
    }

    /**
     * Payment event listener.
     */
    @Service
    public static class PaymentEventListener {

        @QueueListener(
            queue = "payments.processing",
            concurrency = 3,
            autoAck = false
        )
        @Exchange(name = "payments", type = ExchangeType.TOPIC)
        @Binding(exchange = "payments", routingKey = "payment.*")
        public void handlePayment(PaymentEvent event) {
            System.out.println("Payment " + event.paymentId() +
                " for order " + event.orderId() +
                " status: " + event.status());
        }
    }

    /**
     * Notification service with fanout exchange.
     */
    @Service
    @Exchange(name = "notifications", type = ExchangeType.FANOUT)
    public static class NotificationService {

        @RabbitPublisher(exchange = "notifications", deliveryMode = 1)
        private MessagePublisher<NotificationEvent> notificationPublisher;

        public void sendNotification(String userId, String message, String channel) {
            NotificationEvent event = new NotificationEvent(userId, message, channel);
            notificationPublisher.publish(event);
        }
    }

    /**
     * REST API controller.
     */
    @RestController
    public static class OrderController {
        private final OrderService orderService;
        private final PaymentService paymentService;
        private final NotificationService notificationService;

        public OrderController(
            OrderService orderService,
            PaymentService paymentService,
            NotificationService notificationService
        ) {
            this.orderService = orderService;
            this.paymentService = paymentService;
            this.notificationService = notificationService;
        }

        @PostMapping(path = "/api/orders")
        public ResponseEntity<String> createOrder(Request request) {
            String orderId = java.util.UUID.randomUUID().toString();
            double amount = Double.parseDouble(request.queryParam("amount"));

            orderService.createOrder(orderId, amount);
            notificationService.sendNotification(
                "user123",
                "Order " + orderId + " created",
                "email"
            );

            return ResponseEntity.ok("Order created: " + orderId);
        }

        @PostMapping(path = "/api/orders/:id/cancel")
        public ResponseEntity<String> cancelOrder(Request request) {
            String orderId = request.pathParam("id");
            orderService.cancelOrder(orderId);
            return ResponseEntity.ok("Order cancelled: " + orderId);
        }

        @PostMapping(path = "/api/payments")
        public ResponseEntity<String> processPayment(Request request) {
            String paymentId = java.util.UUID.randomUUID().toString();
            String orderId = request.queryParam("orderId");

            paymentService.processPayment(paymentId, orderId);
            return ResponseEntity.ok("Payment processing: " + paymentId);
        }
    }

    @Override
    public void configure(net.magnesiumbackend.core.MagnesiumRuntime runtime) {
        // RabbitMQ configuration is loaded from application.toml
        // See RabbitMQConfiguration class for available properties
    }

    public static void main(String[] args) {
        MagnesiumApplication.run(new RabbitMQExample(), 8080);
    }
}
