package net.goldolphin.maria.api.protoson;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class Response {
    private final String content;

    public Response(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
