package net.goldolphin.maria.api.reflect;

/**
 * Created by caofuxiang on 2017/4/28.
 */
public class ResultOrError {
    private final Object result;
    private final Throwable error;

    private ResultOrError(Object result, Throwable error) {
        this.result = result;
        this.error = error;
    }

    public boolean isError() {
        return error != null;
    }

    public Object getResult() {
        return result;
    }

    public Throwable getError() {
        return error;
    }

    public static ResultOrError fromResult(Object result) {
        return new ResultOrError(result, null);
    }

    public static ResultOrError fromError(Throwable error) {
        return new ResultOrError(null, error);
    }
}
