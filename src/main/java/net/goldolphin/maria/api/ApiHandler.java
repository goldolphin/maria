package net.goldolphin.maria.api;

/**
 * Created by caofuxiang on 2017/4/17.
 */
public interface ApiHandler<REQUEST, RESPONSE> {
    RESPONSE call(REQUEST request);
}
