module net.magnesiumbackend.test {
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
    requires core;
    requires org.mockito;

    exports net.magnesiumbackend.test.config;
    exports net.magnesiumbackend.test.services;
    exports net.magnesiumbackend.test.route;
    exports net.magnesiumbackend.test.health;
    exports net.magnesiumbackend.test.circuit;
    exports net.magnesiumbackend.test.security;
    exports net.magnesiumbackend.test.ratelimit;
    exports net.magnesiumbackend.test.lifecycle;

    opens net.magnesiumbackend.test.config;
    opens net.magnesiumbackend.test.services;
    opens net.magnesiumbackend.test.route;
    opens net.magnesiumbackend.test.health;
    opens net.magnesiumbackend.test.circuit;
    opens net.magnesiumbackend.test.security;
    opens net.magnesiumbackend.test.ratelimit;
    opens net.magnesiumbackend.test.lifecycle;
}