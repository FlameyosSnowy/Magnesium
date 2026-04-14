package net.magnesiumbackend.core.lifecycle;

/**
 * Exception thrown during lifecycle execution.
 *
 * <p>LifecycleException indicates errors in lifecycle initialization including:
 * <ul>
 *   <li>Cyclic dependencies detected at runtime</li>
 *   <li>Missing dependencies that weren't caught at compile time</li>
 *   <li>Component initialization failures</li>
 *   <li>Stage ordering violations</li>
 * </ul>
 * </p>
 *
 * @see LifecycleGraph
 * @see LifecycleRegistry
 */
public class LifecycleException extends RuntimeException {

    private final Class<?> component;
    private final LifecycleGraph.Node node;

    /**
     * Creates a new LifecycleException.
     *
     * @param message the error message
     */
    public LifecycleException(String message) {
        super(message);
        this.component = null;
        this.node = null;
    }

    /**
     * Creates a new LifecycleException with a cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
        this.component = null;
        this.node = null;
    }

    /**
     * Creates a new LifecycleException for a specific component.
     *
     * @param component the component that failed
     * @param message   the error message
     */
    public LifecycleException(Class<?> component, String message) {
        super(message + " [component=" + component.getName() + "]");
        this.component = component;
        this.node = null;
    }

    /**
     * Creates a new LifecycleException for a specific component with cause.
     *
     * @param component the component that failed
     * @param message   the error message
     * @param cause     the underlying cause
     */
    public LifecycleException(Class<?> component, String message, Throwable cause) {
        super(message + " [component=" + component.getName() + "]", cause);
        this.component = component;
        this.node = null;
    }

    /**
     * Creates a new LifecycleException with full context.
     *
     * @param component the component that failed
     * @param node      the lifecycle node information
     * @param message   the error message
     * @param cause     the underlying cause
     */
    public LifecycleException(Class<?> component, LifecycleGraph.Node node,
                             String message, Throwable cause) {
        super(formatMessage(component, node, message), cause);
        this.component = component;
        this.node = node;
    }

    /**
     * Returns the component that caused the error, if known.
     *
     * @return the component class, or null
     */
    public Class<?> getComponent() {
        return component;
    }

    /**
     * Returns the lifecycle node information, if available.
     *
     * @return the node, or null
     */
    public LifecycleGraph.Node getNode() {
        return node;
    }

    private static String formatMessage(Class<?> component, LifecycleGraph.Node node, String message) {
        StringBuilder sb = new StringBuilder(message);
        sb.append(" [component=").append(component.getName()).append("]");
        if (node != null) {
            sb.append(" [stage=").append(node.stage()).append("]");
            if (!node.dependencies().isEmpty()) {
                sb.append(" [dependencies=").append(node.dependencies()).append("]");
            }
        }
        return sb.toString();
    }
}
