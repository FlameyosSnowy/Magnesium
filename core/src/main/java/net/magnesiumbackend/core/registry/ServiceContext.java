package net.magnesiumbackend.core.registry;

import net.magnesiumbackend.core.event.EventBus;

/** Provides access to registered services and framework singletons. */
public interface ServiceContext {
    <T> T get(Class<T> type);
    EventBus eventBus();
}