package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.route.HttpRouteRegistry;

public interface MagnesiumTransport {
    void bind(int port, MagnesiumApplication application, HttpRouteRegistry routes);

    void shutdown();

    int getPort();
}