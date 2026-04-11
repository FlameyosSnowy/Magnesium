package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.HttpStatusCode;

public class ServiceUnavailableException extends HttpExceptionBase {

    public ServiceUnavailableException(String message) {
        super(HttpStatusCode.of(503), message);
    }
}