package net.magnesiumbackend.core.exceptions;

public class MonitorCancellationException extends RuntimeException {
    public MonitorCancellationException(String message) {
        super(message);
    }
}
