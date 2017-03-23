package net.goldolphin.maria.common;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Fix bugs and extend features in {@link io.netty.handler.codec.http.QueryStringEncoder}:
 * 1. Handle empty/null uri correctly.
 * 2. Handle uri with query string correctly.
 * 3. Could be used for POST request.
 * Created by caofuxiang on 2016/9/28.
 */
public class QueryStringEncoder {
    private boolean hasQuery = false;
    private final StringBuilder builder;

    public static void setPostContentType(HttpRequest request) {
        request.headers().add(HttpHeaders.Names.CONTENT_TYPE, "application/x-www-form-urlencoded");
    }

    public QueryStringEncoder() {
        this(null);
    }

    public QueryStringEncoder(String uri) {
        if (uri != null) {
            builder = new StringBuilder(uri);
            hasQuery = (uri.indexOf('?') >= 0);
        } else {
            builder = new StringBuilder();
        }
    }

    public void addParam(String name, String value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (builder.length() > 0) {
            builder.append(hasQuery ? '&' : '?');
        }
        hasQuery = true;
        builder.append(encodeComponent(name, "UTF-8"));
        if (value != null) {
            builder.append('=').append(encodeComponent(value, "UTF-8"));
        }
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    private static String encodeComponent(String s, String enc) {
        // TODO: Optimize me.
        try {
            return URLEncoder.encode(s, enc).replace("+", "%20");
        } catch (UnsupportedEncodingException ignored) {
            throw new UnsupportedCharsetException(enc);
        }
    }
}
