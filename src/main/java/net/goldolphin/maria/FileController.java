package net.goldolphin.maria;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by caofuxiang on 2016/10/26.
 */
public class FileController implements IHttpController {
    private final String baseUriPath;
    private final String baseFileDir;

    public FileController(String baseUriPath, String baseFileDir) {
        this.baseUriPath = baseUriPath;
        this.baseFileDir = baseFileDir;
    }

    @Override
    public void handle(Map<String, String> pathParams, HttpContext context) throws Exception {
        FullHttpRequest request = context.getRequest();

        if (!request.getMethod().equals(HttpMethod.GET)) {
            context.sendError(HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        String uri = sanitizeUri(request.getUri());
        String filePath = getFilePath(uri);
        if (filePath == null) {
            context.sendError(HttpResponseStatus.NOT_FOUND);
            return;
        }

        File file = new File(baseFileDir, filePath);
        if (!file.isFile()) {
            context.sendError(HttpResponseStatus.NOT_FOUND);
            return;
        }

        String contentType = getContentType(filePath);
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
        long fileLength = file.length();
        HttpHeaders.setContentLength(response, fileLength);
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, contentType);

        ChannelHandlerContext underlyingContext = context.getUnderlyingContext();
        underlyingContext.write(response);
        underlyingContext.write(new DefaultFileRegion(file, 0, fileLength));
        ChannelFuture f = underlyingContext.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!(HttpHeaders.isKeepAlive(request) && response.getStatus().code() == HttpResponseStatus.OK.code())) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private String getFilePath(String uri) {
        if (!uri.startsWith(baseUriPath)) {
            return null;
        }
        return uri.substring(baseUriPath.length());
    }

    private static String getContentType(String filename) {
        if (filename.endsWith(".html") || filename.endsWith(".htm")) {
            return "text/html; charset=UTF-8";
        } else if (filename.endsWith(".txt")) {
            return "text/plain; charset=UTF-8";
        } else if (filename.endsWith(".js")) {
            return "application/javascript";
        } else if (filename.endsWith(".css")) {
            return "text/css";
        } else if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "";
    }

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
    private static String sanitizeUri(String uri) {
        int index = uri.indexOf('?');
        if (index >= 0) {
            uri = uri.substring(0, index);
        }

        // Decode the path.
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }

        // Simplistic dumb security check.
        if (uri.contains("/.") ||
                uri.contains("./") ||
                uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.' ||
                INSECURE_URI.matcher(uri).matches()) {
            return null;
        }
        return uri;
    }
}
