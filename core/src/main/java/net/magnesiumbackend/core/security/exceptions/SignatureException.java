package net.magnesiumbackend.core.security.exceptions;

public final class SignatureException extends RuntimeException {
    public SignatureException(String message) { super(message); }
}