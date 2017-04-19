package net.goldolphin.maria.api.reflect;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public interface ErrorHandler<RESPONSE> {
    RESPONSE onError(Throwable throwable);
}
