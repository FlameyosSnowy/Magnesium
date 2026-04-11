package net.magnesiumbackend.core.reload;

@FunctionalInterface
public interface ReloadListener {
    void onReload(ClassLoader newLoader) throws Exception;
}