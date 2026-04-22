module net.magnesiumbackend.redis {
    requires core;
    requires org.jetbrains.annotations;
    requires lettuce.core;
    requires org.apache.commons.pool2;
    requires reactor.core;

    exports net.magnesiumbackend.redis;
    exports net.magnesiumbackend.redis.health;
    exports net.magnesiumbackend.redis.reactive;
    exports net.magnesiumbackend.redis.pubsub;

    provides net.magnesiumbackend.core.extensions.MagnesiumExtension
        with net.magnesiumbackend.redis.RedisExtension;

    provides net.magnesiumbackend.core.health.HealthIndicatorContributor
        with net.magnesiumbackend.redis.RedisHealthContributor;
}
