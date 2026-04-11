package net.magnesiumbackend.core.annotations.service;

import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.registry.HttpRouteRegistry;
import net.magnesiumbackend.core.registry.ServiceRegistry;

public interface GeneratedRouteRegistrationClass {
    void register(MagnesiumApplication application, ServiceRegistry serviceRegistry, HttpRouteRegistry httpRouteRegistry);
}
