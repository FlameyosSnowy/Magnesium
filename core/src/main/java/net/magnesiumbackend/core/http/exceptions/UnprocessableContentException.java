package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

public class UnprocessableContentException extends HttpExceptionBase {

    public UnprocessableContentException(String message) {
        super(HttpStatusCode.of(422), message);
    }
}