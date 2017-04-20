package net.goldolphin.maria.common;

/**
 * Created by caofuxiang on 2017/2/24.
 */
public class ExceptionUtils {
    public static RuntimeException toUnchecked(Throwable e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }
}