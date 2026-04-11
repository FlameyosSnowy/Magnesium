package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

public class UnauthorizedException extends HttpExceptionBase {

    public UnauthorizedException(String message) {
        super(HttpStatusCode.of(401), message);
    }
}