package net.magnesiumbackend.core;

public enum TransportMode {

    /**
     * Framework fully controls lifecycle:
     * - binds automatically during runtime.run()
     * - safest mode
     */
    AUTO,

    /**
     * User signals readiness, but framework still performs binding.
     * Useful for staged startup (DB warmup, cache loading, etc.)
     */
    MANAGED,

    /**
     * User is allowed to manually bind transport.
     *
     * ⚠ Unsafe mode:
     * - bypasses automatic lifecycle guarantees
     * - must only be used for tests, debugging, or embedded use
     */
    MANUAL
}