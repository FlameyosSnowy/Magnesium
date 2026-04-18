package net.magnesiumbackend.core;

import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.RouteTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

final class MagnesiumStartupLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(MagnesiumStartupLogger.class);

    private MagnesiumStartupLogger() {
    }

    private static final String BANNER = """
        
        ███╗   ███╗ █████╗  ██████╗ ███╗   ██╗███████╗███████╗██╗██╗   ██╗███╗   ███╗
        ████╗ ████║██╔══██╗██╔════╝ ████╗  ██║██╔════╝██╔════╝██║██║   ██║████╗ ████║
        ██╔████╔██║███████║██║  ███╗██╔██╗ ██║█████╗  ███████╗██║██║   ██║██╔████╔██║
        ██║╚██╔╝██║██╔══██║██║   ██║██║╚██╗██║██╔══╝  ╚════██║██║██║   ██║██║╚██╔╝██║
        ██║ ╚═╝ ██║██║  ██║╚██████╔╝██║ ╚████║███████╗███████║██║╚██████╔╝██║ ╚═╝ ██║
        ╚═╝     ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚══════╝╚══════╝╚═╝ ╚═════╝ ╚═╝     ╚═╝
        
        """;

    static void logStartup(MagnesiumRuntime app) {
        LOGGER.info(BANNER);

        logRoutes(app);
        logWebSockets(app);
    }

    private static void logRoutes(MagnesiumRuntime app) {
        var registry = app.router().routes();

        LOGGER.info("MAGNESIUM :: ROUTES");

        for (Map.Entry<HttpMethod, RouteTree<RouteDefinition>> entry : registry.trees().entrySet()) {
            HttpMethod method = entry.getKey();
            RouteTree<RouteDefinition> tree = entry.getValue();
            var routes = tree.dump();

            for (var route : routes) {
                LOGGER.info("[{}] {} -> {}",
                    method,
                    route.path(),
                    route.handler().getClass().getSimpleName()
                );
            }
        }

        LOGGER.info("\n");
    }

    private static void logWebSockets(MagnesiumRuntime app) {
        var registry = app.router().webSocketRouteRegistry();

        LOGGER.info("MAGNESIUM :: WEBSOCKETS");

        for (var route : registry.dump()) {
            LOGGER.info("  [WS] {} -> {}",
                route.path(),
                route.handler().getClass().getSimpleName()
            );
        }
    }
}