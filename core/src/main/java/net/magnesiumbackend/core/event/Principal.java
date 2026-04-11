package net.magnesiumbackend.core.event;

import net.magnesiumbackend.core.exceptions.PermissionMismatch;

import java.util.Objects;

/**
 * Represents an authenticated caller.
 *
 * <p>A {@code Principal} is produced once, at the authentication boundary, and
 * carried through the request and onto any events it generates. It intentionally
 * holds no credentials, authentication has already happened by the time this
 * object exists.
 *
 * <p>The type parameter {@code ID} is the application's identity key type
 * (e.g. {@code UUID}, {@code Long}, {@code String}). Using a type parameter
 * rather than always {@code String} lets the compiler catch mismatches between
 * a {@code Principal<UUID>} and a {@code customerId: Long} at compile time
 * rather than at runtime.
 *
 * <pre>{@code
 * // At the auth boundary:
 * Principal<UUID> principal = Principal.of(UUID.fromString(claims.subject()));
 *
 * // In a handler:
 * req.principal().mustOwn(order.customerId()); // throws if mismatch
 *
 * // System-initiated work (scheduled tasks, migrations):
 * Principal<UUID> system = Principal.system();
 * system.isSystem(); // true, mustOwn() is a no-op for system principals
 * }</pre>
 *
 * @param <ID> the type of the identity key
 */
public final class Principal<ID> {

    private final ID id;
    private final boolean system;

    private Principal(ID id, boolean system) {
        this.id     = id;
        this.system = system;
    }

    /**
     * Creates a principal for an authenticated user.
     *
     * @param id the caller's identity key; must not be {@code null}
     */
    public static <ID> Principal<ID> of(ID id) {
        Objects.requireNonNull(id, "Principal id must not be null");
        return new Principal<>(id, false);
    }

    /**
     * Returns the singleton system principal, used for background tasks and
     * internal operations that are not initiated by a human caller.
     *
     * <p>System principals pass {@link #mustOwn} unconditionally.
     */
    @SuppressWarnings("unchecked")
    public static <ID> Principal<ID> system() {
        return (Principal<ID>) SystemHolder.INSTANCE;
    }

    /**
     * Returns the caller's identity key.
     *
     * @throws IllegalStateException if called on a system principal
     */
    public ID id() {
        if (system) throw new IllegalStateException("System principal has no id.");
        return id;
    }

    /** Returns {@code true} if this is a system principal. */
    public boolean isSystem() {
        return system;
    }

    /**
     * Asserts that this principal owns the resource identified by {@code resourceId}.
     *
     * <p>System principals always pass. User principals pass only when their id
     * equals {@code resourceId}.
     *
     * @param resourceId the id of the resource being accessed
     * @throws PermissionMismatch if this is a user principal whose id does not
     *                            equal {@code resourceId}
     */
    public void mustOwn(ID resourceId) {
        if (system) return;
        if (!Objects.equals(this.id, resourceId)) {
            throw new PermissionMismatch(
                "Principal '" + id + "' does not own resource '" + resourceId + "'."
            );
        }
    }

    @Override
    public String toString() {
        return system ? "Principal[SYSTEM]" : "Principal[" + id + "]";
    }

    private static final class SystemHolder {
        static final Principal<?> INSTANCE = new Principal<>(null, true);
    }
}