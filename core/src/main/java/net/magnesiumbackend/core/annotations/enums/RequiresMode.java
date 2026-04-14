package net.magnesiumbackend.core.annotations.enums;

/**
 * Specifies how multiple required permissions are evaluated for access control.
 *
 * <p>Used with {@link net.magnesiumbackend.core.annotations.Requires} annotation to determine
 * whether a user must possess all listed permissions or just one.</p>
 *
 * @see net.magnesiumbackend.core.annotations.Requires
 */
public enum RequiresMode {
    /**
     * The user must possess at least one of the listed permissions.
     *
     * <p>Useful for granting access to multiple distinct roles that can
     * perform the same action (e.g., "admin" OR "moderator" can delete posts).</p>
     */
    ANY,

    /**
     * The user must possess all of the listed permissions.
     *
     * <p>Useful for requiring multiple distinct capabilities to perform an
     * action (e.g., need both "order:read" AND "billing:read" to view invoices).</p>
     */
    ALL
}
