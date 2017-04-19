package net.goldolphin.maria.api;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public interface ApiServerCodec<REQUEST, RESPONSE, ENC_REQUEST, ENC_RESPONSE> {
    REQUEST decodeRequest(ENC_REQUEST encoded);
    ENC_RESPONSE encodeResponse(RESPONSE response);
}
