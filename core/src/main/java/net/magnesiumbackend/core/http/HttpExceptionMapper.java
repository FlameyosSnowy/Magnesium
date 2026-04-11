package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.http.exceptions.HttpException;
import net.magnesiumbackend.core.http.response.HttpStatusCode;

public final class HttpExceptionMapper {

    public static HttpStatusCode map(Throwable t) {

        if (t instanceof HttpException http) {
            return http.status();
        }

        if (t instanceof IllegalArgumentException) {
            return HttpStatusCode.of(400);
        }

        if (t instanceof SecurityException) {
            return HttpStatusCode.of(403);
        }

        return HttpStatusCode.of(500);
    }
}