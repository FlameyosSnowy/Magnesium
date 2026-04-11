package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

public class InternalServerErrorException extends HttpExceptionBase {

    public InternalServerErrorException(String message) {
        super(HttpStatusCode.of(500), message);
    }

    public InternalServerErrorException(String message, Throwable cause) {
        super(HttpStatusCode.of(500), message, cause);
    }
}