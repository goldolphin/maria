package net.goldolphin.maria.serializer;

import java.io.IOException;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;

/**
 * Created by caofuxiang on 2019/05/27.
 */
public interface ProtoSerializer {
    String toString(MessageOrBuilder message) throws IOException;

    <T extends Message.Builder> T fromString(String encoded, T builder) throws IOException;
}
