package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

public class TooManyRequestsException extends HttpExceptionBase {

    public TooManyRequestsException(String message) {
        super(HttpStatusCode.of(429), message);
    }
}