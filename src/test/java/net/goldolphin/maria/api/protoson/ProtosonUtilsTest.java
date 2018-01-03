package net.goldolphin.maria.api.protoson;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.google.protobuf.Message;
import com.google.protobuf.Method;

import net.goldolphin.maria.common.JsonUtils;

public class ProtosonUtilsTest {
    private static void verifySchema(String expectedSchema, String schema) throws IOException {
        JsonUtils.verify(schema);
        System.out.println(schema);
        Assert.assertEquals(expectedSchema, schema);
    }

    @Test
    public void testBuildSchema() throws Exception {
        verifySchema(
                "{\"type\":\"OBJECT\",\"entry\":{\"name\":\"STRING\",\"requestTypeUrl\":\"STRING\",\"requestStreaming\":\"BOOL\"," +
                "\"responseTypeUrl\":\"STRING\",\"responseStreaming\":\"BOOL\",\"options\":{\"type\":\"LIST\",\"entry\":{\"type\":\"OBJECT\"," +
                "\"entry\":{\"name\":\"STRING\",\"value\":{\"type\":\"OBJECT\",\"entry\":{\"typeUrl\":\"STRING\",\"value\":\"BYTES\"}}}}}," +
                "\"syntax\":[\"SYNTAX_PROTO2\",\"SYNTAX_PROTO3\"]}}",
                ProtosonUtils.buildSchemaString(Method.getDefaultInstance()));
        verifySchema(
                "{\"type\":\"OBJECT\",\"entry\":{\"name\":{\"type\":\"STRING\"},\"requestTypeUrl\":{\"type\":\"STRING\"}," +
                        "\"requestStreaming\":{\"type\":\"BOOL\"},\"responseTypeUrl\":{\"type\":\"STRING\"}," +
                        "\"responseStreaming\":{\"type\":\"BOOL\"},\"options\":{\"type\":\"LIST\",\"entry\":{\"type\":\"OBJECT\"," +
                        "\"entry\":{\"name\":{\"type\":\"STRING\"},\"value\":{\"type\":\"OBJECT\",\"entry\":{\"typeUrl\":{\"type\":\"STRING\"}," +
                        "\"value\":{\"type\":\"BYTES\"}}}}}},\"syntax\":{\"type\":\"ENUM\",\"values\":[\"SYNTAX_PROTO2\"," +
                        "\"SYNTAX_PROTO3\"]}}}",
                ProtosonUtils.buildSchemaString(Method.getDefaultInstance(), false));
    }
}