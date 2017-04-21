package net.goldolphin.maria.api.protoson;

import com.google.protobuf.Message;

/**
 * Created by caofuxiang on 2017/4/20.
 */
public interface ResponseVisitor {
    Message getResult(Message response);
    Message getError(Message response);
}
