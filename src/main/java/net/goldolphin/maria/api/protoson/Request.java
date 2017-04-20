package net.goldolphin.maria.api.protoson;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class Request {
    private final String method;
    private final String content;

    public Request(String method, String content) {
        this.method = method;
        this.content = content;
    }

    public String getMethod() {
        return method;
    }

    public String getContent() {
        return content;
    }
}
