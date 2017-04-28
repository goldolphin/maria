package net.goldolphin.maria.api.protoson;

import com.google.protobuf.Message;

/**
 * Created by caofuxiang on 2017/4/28.
 */
public interface ErrorCodec {
    Message encode(Throwable error);
    Throwable decode(Message encoded);
    Message getErrorMessageProtoType();
}
