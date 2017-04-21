package net.goldolphin.maria.api.protoson;

import net.goldolphin.maria.api.ApiServerCodec;

/**
 * Created by caofuxiang on 2017/4/21.
 */
public class CliCodec implements ApiServerCodec<Request, Response, String[], String> {
    @Override
    public Request decodeRequest(String[] encoded) {
        if (encoded.length == 2) {
            return new Request(encoded[0], encoded[1]);
        }
        return new Request(encoded[0], null);
    }

    @Override
    public String encodeResponse(Response response) {
        return response.getContent();
    }
}
