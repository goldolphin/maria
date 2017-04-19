package net.goldolphin.maria.api;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public interface ApiClientCodec<REQUEST, RESPONSE, ENC_REQUEST, ENC_RESPONSE> {
    ENC_REQUEST encodeRequest(REQUEST request);
    RESPONSE decodeResponse(REQUEST request, ENC_RESPONSE encoded);
}
