module net.magnesiumbackend.amqp {
    requires core;
    requires org.jetbrains.annotations;
    requires com.rabbitmq.client;
    requires org.slf4j;

    exports net.magnesiumbackend.amqp;
    exports net.magnesiumbackend.amqp.annotations;
    exports net.magnesiumbackend.amqp.health;

    provides net.magnesiumbackend.core.extensions.MagnesiumExtension
        with net.magnesiumbackend.amqp.RabbitMQExtension;

    provides net.magnesiumbackend.core.health.HealthIndicatorContributor
        with net.magnesiumbackend.amqp.RabbitMQHealthContributor;

    uses net.magnesiumbackend.amqp.RabbitMQWiring;
}
