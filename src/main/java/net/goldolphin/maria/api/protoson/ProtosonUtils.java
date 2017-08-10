package net.goldolphin.maria.api.protoson;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;

import net.goldolphin.maria.common.ExceptionUtils;
import net.goldolphin.maria.common.JsonUtils;

/**
 * Created by caofuxiang on 2017/4/20.
 */
public class ProtosonUtils {
    public static Stream<Method> readInterface(Class<?> interfaceClass) {
        return Arrays.stream(interfaceClass.getMethods()).filter(method -> !Modifier.isStatic(method.getModifiers()) && !method.isDefault());
    }

    public static boolean isAsync(Method method) {
        Class<?> returnType = method.getReturnType();
        return returnType.equals(CompletableFuture.class);
    }

    public static Message getResponsePrototype(Method method) {
        try {
            Class<?> actualClass = isAsync(method)
                    ? (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0]
                    : method.getReturnType();
            if (actualClass.equals(Void.class) || actualClass.equals(Void.TYPE)) {
                return null;
            }
            Method getDefaultInstance = actualClass.getMethod("getDefaultInstance");
            return (Message) getDefaultInstance.invoke(null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid method: " + method.toGenericString());
        }
    }

    public static Message getRequestPrototype(Method method) {
        try {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length == 0) {
                return null;
            }
            if (paramTypes.length != 1) {
                throw new IllegalArgumentException();
            }
            Class<?> actualClass = paramTypes[0];
            Method getDefaultInstance = actualClass.getMethod("getDefaultInstance");
            return (Message) getDefaultInstance.invoke(null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid method: " + method.toGenericString());
        }
    }

    public static String buildSchemaString(Message message) {
        StringWriter writer = new StringWriter();
        try {
            buildSchema(JsonUtils.factory().createGenerator(writer), message);
            return writer.toString();
        } catch (IOException e) {
            throw ExceptionUtils.toUnchecked(e);
        }
    }

    public static void buildSchema(JsonGenerator generator, Message message) throws IOException {
        if (message == null) {
            generator.writeNull();
        } else if (message instanceof BoolValue) {
            generator.writeString(Descriptors.FieldDescriptor.Type.BOOL.toString());
        } else if (message instanceof Int32Value) {
            generator.writeString(Descriptors.FieldDescriptor.Type.INT32.toString());
        } else if (message instanceof UInt32Value) {
            generator.writeString(Descriptors.FieldDescriptor.Type.UINT32.toString());
        } else if (message instanceof Int64Value) {
            generator.writeString(Descriptors.FieldDescriptor.Type.INT64.toString());
        } else if (message instanceof UInt64Value) {
            generator.writeString(Descriptors.FieldDescriptor.Type.UINT64.toString());
        } else if (message instanceof StringValue) {
            generator.writeString(Descriptors.FieldDescriptor.Type.STRING.toString());
        } else if (message instanceof BytesValue) {
            generator.writeString(Descriptors.FieldDescriptor.Type.BYTES.toString());
        } else if (message instanceof FloatValue) {
            generator.writeString(Descriptors.FieldDescriptor.Type.FLOAT.toString());
        } else if (message instanceof DoubleValue) {
            generator.writeString(Descriptors.FieldDescriptor.Type.DOUBLE.toString());
        } else if (message instanceof Timestamp) {
            generator.writeString(Descriptors.FieldDescriptor.Type.STRING.toString());
        } else {
            buildSchema(generator, message.getDescriptorForType());
        }
        generator.flush();
    }

    private static void buildSchema(JsonGenerator generator, Descriptors.Descriptor descriptor) throws IOException {
        generator.writeStartObject();
        for (Descriptors.FieldDescriptor f: descriptor.getFields()) {
            generator.writeFieldName(f.getJsonName());
            if (f.isRepeated()) {
                generator.writeStartArray();
                if (f.isMapField()) {
                    generator.writeString("MAP");
                } else {
                    generator.writeString("LIST");
                }
                appendFieldValue(generator, f);
                generator.writeEndArray();
            } else {
                appendFieldValue(generator, f);
            }
        }
        generator.writeEndObject();
    }

    private static void appendFieldValue(JsonGenerator generator, Descriptors.FieldDescriptor field) throws IOException {
        Descriptors.FieldDescriptor.Type t = field.getType();
        if (t == Descriptors.FieldDescriptor.Type.MESSAGE) {
            buildSchema(generator, field.getMessageType());
        } else if (t == Descriptors.FieldDescriptor.Type.ENUM) {
            generator.writeStartArray();
            generator.writeString("ENUM");
            for (Descriptors.EnumValueDescriptor v: field.getEnumType().getValues()) {
                generator.writeString(v.toString());
            }
            generator.writeEndArray();
        } else {
            generator.writeString(t.toString());
        }
    }
}
