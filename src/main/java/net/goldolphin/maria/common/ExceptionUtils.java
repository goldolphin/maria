package net.goldolphin.maria.common;

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
        while (true) {
            if (e.getCause() == null) {
                return e;
            }
            e = e.getCause();
        }
    }
}
