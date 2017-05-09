package net.goldolphin.maria;

import java.io.IOException;

/**
 * Created by caofuxiang on 2017/5/9.
 */
public class HttpException extends IOException {
    public HttpException() {
    }

    public HttpException(String message) {
        super(message);
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpException(Throwable cause) {
        super(cause);
    }
}
