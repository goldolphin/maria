package net.goldolphin.maria;

import net.goldolphin.maria.pattern.MatchResult;
import net.goldolphin.maria.pattern.UrlMatcher;
import io.netty.handler.codec.http.FullHttpRequest;

import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

/**
 * Dispatch HTTP requests.
 * @author caofuxiang
 *         2014-03-05 17:34
 */
public class HttpDispatcher {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HttpDispatcher.class);
    private static final IHttpController NOT_FOUND_CONTROLLER = (pathParams, context) -> context.sendNotFound();

    private final UrlMatcher<IHttpController> controllerMatcher = UrlMatcher.newInstance();
    private final IHttpController defaultController;

    public HttpDispatcher() {
        this(NOT_FOUND_CONTROLLER);
    }

    public HttpDispatcher(IHttpController defaultController) {
        this.defaultController = defaultController;
    }

    public void dispatch(HttpContext context) throws Exception {
        FullHttpRequest request = context.getRequest();
        URI uri = URI.create(request.getUri());
        logger.debug("Path={}, Request={}.", uri, request);
        String path = uri.getPath();
        MatchResult<IHttpController> matchResult = controllerMatcher.match(path);
        IHttpController controller = matchResult.isMatched() ? matchResult.getData() : defaultController;
        controller.handle(matchResult.getParameters(), context);
    }

    /**
     * Register resource controller
     * @param pattern refer to {@link UrlMatcher#addPattern(String, Object)} for details.
     * @param controller
     */
    public void registerController(String pattern, IHttpController controller) {
        controllerMatcher.addPattern(pattern, controller);
    }
}
