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
        return buildSchemaString(message, true);
    }

    public static String buildSchemaString(Message message, boolean simplified) {
        StringWriter writer = new StringWriter();
        try {
            appendSchema(JsonUtils.factory().createGenerator(writer), message, simplified);
            return writer.toString();
        } catch (IOException e) {
            throw ExceptionUtils.toUnchecked(e);
        }
    }

    public static void appendSchema(JsonGenerator generator, Message message, boolean simplified) throws IOException {
        if (message == null) {
            generator.writeNull();
        } else if (message instanceof BoolValue) {
            appendScalarSchema(generator, Descriptors.FieldDescriptor.Type.BOOL.toString(), simplified);
        } else if (message instanceof Int32Value) {
            appendScalarSchema(generator, Descriptors.FieldDescriptor.Type.INT32.toString(), simplified);
        } else if (message instanceof UInt32Value) {
            appendScalarSchema(generator, Descriptors.FieldDescriptor.Type.UINT32.toString(), simplified);
        } else if (message instanceof Int64Value) {
            appendScalarSchema(generator, Descriptors.FieldDescriptor.Type.INT64.toString(), simplified);
        } else if (message instanceof UInt64Value) {
            appendScalarSchema(generator, Descriptors.FieldDescriptor.Type.UINT64.toString(), simplified);
        } else if (message instanceof StringValue) {
            appendScalarSchema(generator, Descriptors.FieldDescriptor.Type.STRING.toString(), simplified);
        } else if (message instanceof BytesValue) {
            appendScalarSchema(generator, Descriptors.FieldDescriptor.Type.BYTES.toString(), simplified);
        } else if (message instanceof FloatValue) {
            appendScalarSchema(generator, Descriptors.FieldDescriptor.Type.FLOAT.toString(), simplified);
        } else if (message instanceof DoubleValue) {
            appendScalarSchema(generator, Descriptors.FieldDescriptor.Type.DOUBLE.toString(), simplified);
        } else if (message instanceof Timestamp) {
            appendScalarSchema(generator, Descriptors.FieldDescriptor.Type.STRING.toString(), simplified);
        } else {
            appendObjectSchema(generator, message.getDescriptorForType(), simplified);
        }
        generator.flush();
    }

    private static void appendEntrySchema(JsonGenerator generator, Descriptors.FieldDescriptor descriptor, boolean simplified) throws IOException {
        Descriptors.FieldDescriptor.Type type = descriptor.getType();
        if (type == Descriptors.FieldDescriptor.Type.MESSAGE) {
            appendObjectSchema(generator, descriptor.getMessageType(), simplified);
        } else if (type == Descriptors.FieldDescriptor.Type.ENUM) {
            appendEnumSchema(generator, descriptor.getEnumType(), simplified);
        } else {
            appendScalarSchema(generator, type.name(), simplified);
        }
    }

    private static void appendObjectSchema(JsonGenerator generator, Descriptors.Descriptor descriptor, boolean simplified) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("type");
        generator.writeString("OBJECT");
        generator.writeFieldName("entry");
        generator.writeStartObject();
        for (Descriptors.FieldDescriptor field: descriptor.getFields()) {
            generator.writeFieldName(field.getJsonName());
            if (field.isRepeated()) {
                if (field.isMapField()) {
                    appendMapSchema(generator, field, simplified);
                } else {
                    appendListSchema(generator, field, simplified);
                }
            } else {
                appendEntrySchema(generator, field, simplified);
            }
        }
        generator.writeEndObject();
        generator.writeEndObject();
    }

    private static void appendMapSchema(JsonGenerator generator, Descriptors.FieldDescriptor descriptor, boolean simplified) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("type");
        generator.writeString("MAP");
        generator.writeFieldName("entry");
        generator.writeStartObject();
        Descriptors.Descriptor entry = descriptor.getMessageType();
        generator.writeFieldName("key");
        appendEntrySchema(generator, entry.findFieldByName("key"), simplified);
        generator.writeFieldName("value");
        appendEntrySchema(generator, entry.findFieldByName("value"), simplified);
        generator.writeEndObject();
        generator.writeEndObject();
    }

    private static void appendListSchema(JsonGenerator generator, Descriptors.FieldDescriptor descriptor, boolean simplified) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("type");
        generator.writeString("LIST");
        generator.writeFieldName("entry");
        appendEntrySchema(generator, descriptor, simplified);
        generator.writeEndObject();
    }

    private static void appendEnumSchema(JsonGenerator generator, Descriptors.EnumDescriptor descriptor, boolean simplified) throws IOException {
        if (simplified) {
            generator.writeStartArray();
            for (Descriptors.EnumValueDescriptor v: descriptor.getValues()) {
                generator.writeString(v.toString());
            }
            generator.writeEndArray();
        } else {
            generator.writeStartObject();
            generator.writeFieldName("type");
            generator.writeString("ENUM");
            generator.writeFieldName("values");
            generator.writeStartArray();
            for (Descriptors.EnumValueDescriptor v: descriptor.getValues()) {
                generator.writeString(v.toString());
            }
            generator.writeEndArray();
            generator.writeEndObject();
        }
    }

    private static void appendScalarSchema(JsonGenerator generator, String type, boolean simplified) throws IOException {
        if (simplified) {
            generator.writeString(type);
        } else {
            generator.writeStartObject();
            generator.writeFieldName("type");
            generator.writeString(type);
            generator.writeEndObject();
        }
    }
}
