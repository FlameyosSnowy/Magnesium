package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.HttpStatusCode;

public interface HttpException {
    HttpStatusCode status();
}