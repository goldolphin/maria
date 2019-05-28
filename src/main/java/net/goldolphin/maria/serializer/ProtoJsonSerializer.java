package net.goldolphin.maria.serializer;

import java.io.IOException;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

/**
 * Created by caofuxiang on 2019/05/27.
 */
public class ProtoJsonSerializer implements ProtoSerializer {
    private final JsonFormat.Printer printer;
    private final JsonFormat.Parser parser;

    public ProtoJsonSerializer(JsonFormat.Printer printer, JsonFormat.Parser parser) {
        this.printer = printer;
        this.parser = parser;
    }

    @Override
    public String toString(MessageOrBuilder message) throws IOException {
        return printer.print(message);
    }

    @Override
    public <T extends Message.Builder> T fromString(String encoded, T builder) throws IOException {
        parser.merge(encoded, builder);
        return builder;
    }
}
