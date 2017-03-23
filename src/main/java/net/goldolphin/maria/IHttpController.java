package net.goldolphin.maria;

import java.util.Map;

/**
 * Process http requests, singleton in one service.
 * @author caofuxiang
 *         2014-03-05 20:10
 */
public interface IHttpController {

    /**
     * @param pathParams
     * @param context
     * @throws Exception
     */
    public void handle(Map<String, String> pathParams, HttpContext context) throws Exception;
}
