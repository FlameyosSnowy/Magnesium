package net.magnesiumbackend.core.exceptions;

public class PermissionMismatch extends RuntimeException {
    public PermissionMismatch(String message) {
        super(message);
    }
}
