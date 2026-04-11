package net.magnesiumbackend.transport.tomcat;

import jakarta.websocket.server.ServerEndpointConfig;
import net.magnesiumbackend.core.http.socket.WebSocketHandler;
import net.magnesiumbackend.core.http.socket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.http.socket.WebSocketSessionManager;
import net.magnesiumbackend.core.route.RouteTree;

import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;

public class TomcatWebSocketInitializer {
    private final WebSocketRouteRegistry routeRegistry;
    private final WebSocketSessionManager sessionManager;

    public TomcatWebSocketInitializer(
        WebSocketRouteRegistry routeRegistry,
        WebSocketSessionManager sessionManager
    ) {
        this.routeRegistry  = routeRegistry;
        this.sessionManager = sessionManager;
    }

    public void initialize(ServerContainer serverContainer) throws DeploymentException {
        for (RouteTree.RouteEntry<WebSocketHandler> entry : routeRegistry.entries()) {
            String path = entry.path();

            ServerEndpointConfig config = ServerEndpointConfig.Builder
                .create(TomcatWebSocketEndpoint.class, toJsrPath(path))
                .configurator(new TomcatWebSocketConfigurator(
                    entry.handler(), sessionManager, path
                ))
                .build();

            serverContainer.addEndpoint(config);
        }
    }

    private String toJsrPath(String magnesiumPath) {
        return magnesiumPath;
    }
}