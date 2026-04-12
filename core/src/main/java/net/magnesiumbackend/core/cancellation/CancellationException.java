package net.magnesiumbackend.core.cancellation;

/**
 * Exception thrown when an operation is cancelled via {@link CancellationToken}.
 *
 * <p>This is a runtime exception to allow easy propagation through async chains
 * without checked exception handling.</p>
 */
public class CancellationException extends RuntimeException {

    public CancellationException(String message) {
        super(message);
    }

    public CancellationException(String message, Throwable cause) {
        super(message, cause);
    }
}
