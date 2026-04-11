package net.magnesiumbackend.core.annotations.service;

import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.http.socket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.registry.ServiceRegistry;

public interface GeneratedWebSocketRegistrationClass {
    void register(
        MagnesiumApplication application,
        ServiceRegistry serviceRegistry,
        WebSocketRouteRegistry webSocketRouteRegistry
    );
}