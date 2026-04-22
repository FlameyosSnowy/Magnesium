module net.magnesiumbackend.actuator {

    requires org.jetbrains.annotations;
    requires java.logging;
    requires core;
    requires java.management;

    exports net.magnesiumbackend.actuator;
    exports net.magnesiumbackend.actuator.health;
    exports net.magnesiumbackend.actuator.metrics;

    provides net.magnesiumbackend.core.extensions.MagnesiumExtension
        with net.magnesiumbackend.actuator.ActuatorExtension;
}
