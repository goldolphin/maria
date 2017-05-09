package net.goldolphin.maria.api;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Created by caofuxiang on 2017/5/9.
 */
public class InvalidHttpStatusException extends RuntimeException {
    private HttpResponseStatus status;

    public InvalidHttpStatusException(HttpResponseStatus status) {
        this(status, status.reasonPhrase());
    }

    public InvalidHttpStatusException(HttpResponseStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }
}
