package net.magnesiumbackend.core.auth;

import java.util.Set;

/**
 * Represents an authenticated user or system principal in the Magnesium security model.
 *
 * <p>Principals carry identity information and permissions for authorization decisions.
 * They are created by {@link AuthenticationProvider} implementations during the
 * authentication process and stored in the security context for the request duration.</p>
 *
 * <p>The principal interface supports:
 * <ul>
 *   <li>User identification via {@code userId} and {@code username}</li>
 *   <li>Permission-based authorization via {@code permissions}</li>
 *   <li>Anonymous users for unauthenticated requests</li>
 *   <li>Resource ownership verification</li>
 * </ul>
 * </p>
 *
 * <p>Example usage in a controller:</p>
 * <pre>{@code
 * @RestController
 * public class OrderController {
 *     @GetMapping(path = "/orders/{id}")
 *     @Authenticated
 *     public ResponseEntity<Order> getOrder(@PathParam String id, Principal principal) {
 *         Order order = orderService.findById(id);
 *
 *         // Check resource ownership
 *         principal.mustOwn(order.userId(), () -> {
 *             throw new ForbiddenException("You don't own this order");
 *         });
 *
 *         return ResponseEntity.ok(order);
 *     }
 * }
 * }</pre>
 *
 * @see AuthenticationProvider
 * @see net.magnesiumbackend.core.annotations.Authenticated
 * @see net.magnesiumbackend.core.annotations.Requires
 */
public interface Principal {

    /**
     * Returns the unique identifier for this principal.
     *
     * @return the user ID (empty for anonymous principals)
     */
    String userId();

    /**
     * Returns the human-readable username for this principal.
     *
     * @return the username (empty for anonymous principals)
     */
    String username();

    /**
     * Returns the set of permissions granted to this principal.
     *
     * <p>Permissions are arbitrary strings interpreted by the application,
     * commonly in formats like "resource:action" or simple role names.</p>
     *
     * @return the granted permissions (empty set for anonymous principals)
     */
    Set<String> permissions();

    /**
     * Checks if this principal has the specified permission.
     *
     * @param permission the permission to check
     * @return true if the permission is granted
     */
    default boolean hasPermission(String permission) {
        return permissions().contains(permission);
    }

    /**
     * Verifies that this principal owns a resource, running the error action if not.
     *
     * <p>Convenience method for resource ownership checks. If the principal's
     * userId does not match the resourceOwnerId, the error action is executed.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * principal.mustOwn(order.getUserId(), () -> {
     *     throw new ForbiddenException("Not your order");
     * });
     * }</pre>
     *
     * @param resourceOwnerId the expected owner ID
     * @param ifError         the action to run if ownership check fails
     */
    default void mustOwn(String resourceOwnerId, Runnable ifError) {
        if (!userId().equals(resourceOwnerId)) {
            ifError.run();
        }
    }

    /**
     * Creates an anonymous principal with no identity or permissions.
     *
     * @return an anonymous principal instance
     */
    static Principal anonymous() {
        return new SimplePrincipal("", "", Set.of(), true);
    }

    /**
     * Creates a principal with the specified identity and permissions.
     *
     * @param userId      the unique user identifier
     * @param username    the human-readable username
     * @param permissions the set of granted permissions
     * @return a new principal instance
     */
    static Principal of(String userId, String username, Set<String> permissions) {
        return new SimplePrincipal(userId, username, permissions);
    }

    /**
     * Returns true if this is an anonymous (unauthenticated) principal.
     *
     * @return true if anonymous
     */
    boolean isAnonymous();
}