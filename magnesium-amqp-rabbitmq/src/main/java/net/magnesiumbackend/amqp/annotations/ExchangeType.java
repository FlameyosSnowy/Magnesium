package net.magnesiumbackend.amqp.annotations;

/**
 * RabbitMQ exchange types.
 */
public enum ExchangeType {
    DIRECT,
    FANOUT,
    TOPIC,
    HEADERS
}
