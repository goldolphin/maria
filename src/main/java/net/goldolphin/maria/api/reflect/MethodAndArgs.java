package net.goldolphin.maria.api.reflect;

import java.lang.reflect.Method;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class MethodAndArgs {
    private final Method method;
    private final Object[] args;

    public MethodAndArgs(Method method, Object... args) {
        this.method = method;
        this.args = args;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }
}
