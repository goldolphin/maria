package net.goldolphin.maria;

import net.goldolphin.maria.common.MessageUtils;
import net.goldolphin.maria.restful.RestfulController;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author caofuxiang
 *         2016-04-05 11:03:03.
 */
public class HttpServerTest {
    @Test
    public void testRestful() throws Exception {
        HttpDispatcher dispatcher = new HttpDispatcher();
        dispatcher.registerController("/hi", new RestfulController() {
            @Override
            protected void get(Map<String, String> pathParams, HttpContext context) throws Exception {
                context.send("Hello, world!");
            }

            @Override
            protected void put(Map<String, String> pathParams, HttpContext context) throws Exception {
                FullHttpRequest request = context.getRequest();
                HttpHeaders headers = request.headers();
                for (Map.Entry<String, String> h: headers) {
                    System.out.println(h);
                }
                byte[] bytes = new byte[request.content().readableBytes()];
                request.content().readBytes(bytes);
                String received = new String(bytes);
                System.out.println("Server: " + received);
                context.send("application/oms", bytes);
            }
        });

        HttpServer server = new HttpServer(new InetSocketAddress(6060), dispatcher);
        HttpClient client = new HttpClient(server.getWorkerGroup());
        server.start();

        // Get
        HttpRequest httpGet = MessageUtils.newHttpRequest(HttpMethod.GET, "http://localhost:6060/hi?q=s");
        FullHttpResponse response = client.execute(httpGet).thenApply(FullHttpResponse::copy).get();
        Assert.assertEquals(HttpResponseStatus.OK, response.getStatus());
        Assert.assertEquals("Hello, world!", response.content().toString(CharsetUtil.UTF_8));

        // Post
        HttpRequest httpPost = MessageUtils.newHttpRequest(HttpMethod.POST, "http://localhost:6060/hi");
        response = client.execute(httpPost).thenApply(FullHttpResponse::copy).get();
        Assert.assertEquals(HttpResponseStatus.NOT_FOUND, response.getStatus());

        // Put
        String request = "request";
        HttpRequest httpPut = MessageUtils.newHttpRequest(HttpMethod.PUT, "http://localhost:6060/hi", request);
        response = client.execute(httpPut).thenApply(FullHttpResponse::copy).get();
        Assert.assertEquals(HttpResponseStatus.OK, response.getStatus());
        String received = response.content().toString(CharsetUtil.UTF_8);
        System.out.println("Client: " + received);
        Assert.assertEquals(request, received);

        server.stop(true);
    }

    @Test
    public void testURI() throws MalformedURLException, URISyntaxException {
        URI uri = new URI("http://localhost:6060/abc/def/?aaa&bbb");
        Assert.assertEquals("http://localhost:6060/abc/def/?aaa&bbb", uri.toString());
        Assert.assertEquals("/abc/def/", uri.getPath());
        Assert.assertEquals("aaa&bbb", uri.getQuery());
    }
}
