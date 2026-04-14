package net.magnesiumbackend.core.http.response;

import net.magnesiumbackend.core.headers.HttpHeaderIndex;

record DefaultResponseEntity<T>(int statusCode, T body, HttpHeaderIndex headers) implements ResponseEntity<T> {
}