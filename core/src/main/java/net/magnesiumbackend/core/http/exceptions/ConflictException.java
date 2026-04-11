package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

public class ConflictException extends HttpExceptionBase {

    public ConflictException(String message) {
        super(HttpStatusCode.of(409), message);
    }
}