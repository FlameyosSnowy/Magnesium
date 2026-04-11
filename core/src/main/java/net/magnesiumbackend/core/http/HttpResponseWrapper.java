package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.http.exceptions.HttpExceptionBase;

public final class HttpResponseWrapper {

    private final HttpStatusCode status;

    public HttpResponseWrapper(int rawStatus) {
        this.status = HttpStatusCode.of(rawStatus);
    }

    public boolean isError() {
        return status.isError();
    }

    public void throwIfError() {
        if (isError()) {
            throw new HttpExceptionBase(status, "HTTP error: " + status);
        }
    }
}