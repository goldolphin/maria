package net.goldolphin.maria.common;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author caofuxiang
 *         2016-04-06 20:14:14.
 */
public class CookieUtils {
    public static final long SECONDS_ONE_YEAR = 3600L*24*365;
    public static final long SECONDS_INFINITY = SECONDS_ONE_YEAR * 1000;

    public static Set<Cookie> decodeRequestCookies(HttpRequest request) {
        String header = request.headers().get(HttpHeaders.Names.COOKIE);
        if (header == null) {
            return Collections.emptySet();
        }
        return ServerCookieDecoder.LAX.decode(header);
    }

    public static void encodeRequestCookies(HttpRequest request, Cookie ... cookies) {
        request.headers().set(HttpHeaders.Names.COOKIE, ClientCookieEncoder.STRICT.encode(cookies));
    }

    public static void encodeRequestCookies(HttpRequest request, Collection<? extends Cookie> cookies) {
        request.headers().set(HttpHeaders.Names.COOKIE, ClientCookieEncoder.STRICT.encode(cookies));
    }

    public static Cookie decodeResponseCookie(HttpResponse response) {
        String header = response.headers().get(HttpHeaders.Names.SET_COOKIE);
        if (header == null) {
            return null;
        }
        return ClientCookieDecoder.LAX.decode(header);
    }

    public static void encodeResponseCookie(HttpResponse response, Cookie cookie) {
        response.headers().add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
    }
}
