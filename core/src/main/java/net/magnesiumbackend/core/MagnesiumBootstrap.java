package net.magnesiumbackend.core;

import net.magnesiumbackend.core.annotations.service.GeneratedEmitProxyClass;
import net.magnesiumbackend.core.annotations.service.GeneratedExceptionHandlerClass;
import net.magnesiumbackend.core.annotations.service.GeneratedRouteRegistrationClass;
import net.magnesiumbackend.core.annotations.service.GeneratedServiceClass;
import net.magnesiumbackend.core.annotations.service.GeneratedSubscriberClass;
import net.magnesiumbackend.core.annotations.service.GeneratedWebSocketRegistrationClass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

public record MagnesiumBootstrap(
    List<GeneratedSubscriberClass> subscribers,
    List<GeneratedEmitProxyClass> emitProxies,
    List<GeneratedExceptionHandlerClass> exceptionHandlers,
    List<GeneratedRouteRegistrationClass> routes,
    List<GeneratedWebSocketRegistrationClass> webSockets,
    List<GeneratedServiceClass> services
) {

    public static MagnesiumBootstrap load() {
        return new MagnesiumBootstrap(
            loadAll(GeneratedSubscriberClass.class),
            loadAll(GeneratedEmitProxyClass.class),
            loadAll(GeneratedExceptionHandlerClass.class),
            loadAll(GeneratedRouteRegistrationClass.class),
            loadAll(GeneratedWebSocketRegistrationClass.class),
            loadAll(GeneratedServiceClass.class)
        );
    }

    private static <T> List<T> loadAll(Class<T> type) {
        List<T> providers = new ArrayList<>(1);
        for (T loaded : ServiceLoader.load(type)) {
            providers.add(loaded);
        }
        return providers;
    }

    public void apply(MagnesiumRuntime app) {
        var services = app.serviceRegistry();
        var eventBus = app.eventBus();

        for (var s : subscribers) {
            s.register(app, services, eventBus.subscribeRegistry());
        }

        for (var e : emitProxies) {
            Object proxy = e.create(app, services, eventBus.emitRegistry());
            services.replaceInstance(e.serviceType(), proxy);
        }

        for (var ex : exceptionHandlers) {
            ex.register(app, services);
        }

        for (var r : routes) {
            r.register(app, services, app.router().routes());
        }

        for (var ws : webSockets) {
            ws.register(app, services, app.router().webSocketRouteRegistry());
        }
    }
}