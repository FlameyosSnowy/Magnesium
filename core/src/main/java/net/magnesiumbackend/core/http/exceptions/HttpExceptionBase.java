package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.HttpStatusCode;

public class HttpExceptionBase extends RuntimeException implements HttpException {

    private final HttpStatusCode status;

    public HttpExceptionBase(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }

    public HttpExceptionBase(HttpStatusCode status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    @Override
    public HttpStatusCode status() {
        return status;
    }
}