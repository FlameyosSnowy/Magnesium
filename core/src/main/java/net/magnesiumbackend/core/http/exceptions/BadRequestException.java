package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.HttpStatusCode;

public class BadRequestException extends HttpExceptionBase {

    public BadRequestException(String message) {
        super(HttpStatusCode.of(400), message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(HttpStatusCode.of(400), message, cause);
    }
}