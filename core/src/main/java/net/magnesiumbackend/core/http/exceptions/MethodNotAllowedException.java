package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.HttpStatusCode;

public class MethodNotAllowedException extends HttpExceptionBase {

    public MethodNotAllowedException(String message) {
        super(HttpStatusCode.of(405), message);
    }
}