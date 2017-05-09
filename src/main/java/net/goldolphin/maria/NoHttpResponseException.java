package net.goldolphin.maria;

/**
 * Created by caofuxiang on 2017/5/9.
 */
public class NoHttpResponseException extends HttpException {
    public NoHttpResponseException(String message) {
        super(message);
    }
}
