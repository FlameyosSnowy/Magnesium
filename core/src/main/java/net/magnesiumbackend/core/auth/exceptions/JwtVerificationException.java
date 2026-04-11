package net.magnesiumbackend.core.auth.exceptions;

public class JwtVerificationException extends RuntimeException {
    public JwtVerificationException(String message) {
        super(message);
    }
}
