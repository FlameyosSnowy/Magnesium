package net.magnesiumbackend.core.annotations.service;

import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.http.websocket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.services.ServiceRegistry;

public interface GeneratedWebSocketRegistrationClass {
    void register(
        MagnesiumApplication application,
        ServiceRegistry serviceRegistry,
        WebSocketRouteRegistry webSocketRouteRegistry
    );
}