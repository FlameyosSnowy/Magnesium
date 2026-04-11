package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

public interface HttpException {
    HttpStatusCode status();
}