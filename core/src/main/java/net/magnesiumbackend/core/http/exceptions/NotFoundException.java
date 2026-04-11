package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.HttpStatusCode;

public class NotFoundException extends HttpExceptionBase {

    public NotFoundException(String message) {
        super(HttpStatusCode.of(404), message);
    }
}