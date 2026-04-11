package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.HttpStatusCode;

public class ForbiddenException extends HttpExceptionBase {

    public ForbiddenException(String message) {
        super(HttpStatusCode.of(403), message);
    }
}