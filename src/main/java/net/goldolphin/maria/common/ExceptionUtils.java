package net.goldolphin.maria.common;

import com.google.common.base.Throwables;

/**
 * Created by caofuxiang on 2017/2/24.
 */
public class ExceptionUtils {
    private ExceptionUtils() { }

    public static RuntimeException toUnchecked(Throwable e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }

    public static Throwable getRootCause(Throwable e) {
        return Throwables.getRootCause(e);
    }
}
