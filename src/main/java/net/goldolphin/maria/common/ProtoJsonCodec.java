package net.goldolphin.maria.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

/**
 * Created by caofuxiang on 2017/3/3.
 */
public class ProtoJsonCodec {
    private static final JsonFormat.Printer PRINTER = JsonFormat.printer()
            .includingDefaultValueFields().omittingInsignificantWhitespace();
    private static final JsonFormat.Parser PARSER = JsonFormat.parser().ignoringUnknownFields();
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static String toString(MessageOrBuilder message) {
        try {
            return PRINTER.print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e); // Should not occur.
        }
    }

    public static <T extends Message.Builder> T fromString(String encoded, T builder) throws
            InvalidProtocolBufferException {
        PARSER.merge(encoded, builder);
        return builder;
    }

    public static byte[] toBytes(MessageOrBuilder message) {
        return toString(message).getBytes(UTF8);
    }

    public static <T extends Message.Builder> T fromBytes(byte[] encoded, T builder) throws
            InvalidProtocolBufferException {
        return fromString(new String(encoded, UTF8), builder);
    }

    public static <T extends Message.Builder> T fromBytes(InputStream input, T builder) throws
            IOException {
        PARSER.merge(new InputStreamReader(input, UTF8), builder);
        return builder;
    }
}
