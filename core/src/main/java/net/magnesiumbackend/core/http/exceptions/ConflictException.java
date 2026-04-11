package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.HttpStatusCode;

public class ConflictException extends HttpExceptionBase {

    public ConflictException(String message) {
        super(HttpStatusCode.of(409), message);
    }
}