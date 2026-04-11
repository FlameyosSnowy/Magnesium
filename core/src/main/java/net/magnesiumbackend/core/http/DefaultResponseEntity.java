package net.magnesiumbackend.core.http;

import java.util.Map;

record DefaultResponseEntity<T>(int statusCode, T body, Map<String, String> headers) implements ResponseEntity<T> {
}