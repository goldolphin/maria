package net.goldolphin.maria.api.protoson;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

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
        if (message == null) {
            return "void";
        }
        StringBuilder builder = new StringBuilder();
        buildSchema(message.getDescriptorForType(), builder);
        return builder.toString();
    }

    private static void buildSchema(Descriptors.Descriptor descriptor, StringBuilder builder) {
        List<Descriptors.FieldDescriptor> fields = descriptor.getFields();
        if (fields.size() == 0) {
            builder.append(descriptor.getName());
            return;
        }
        builder.append("{");
        for (int i = 0; i < fields.size(); ++i) {
            if (i > 0) {
                builder.append(",");
            }
            Descriptors.FieldDescriptor f = fields.get(i);
            builder.append(f.getName()).append(":");
            if (f.isRepeated()) {
                builder.append("[");
                if (f.isMapField()) {
                    builder.append("\"MAP\",");
                } else {
                    builder.append("\"LIST\",");
                }
                appendFieldValue(f, builder);
                builder.append("]");
            } else {
                appendFieldValue(f, builder);
            }
        }
        builder.append("}");
    }

    private static void appendFieldValue(Descriptors.FieldDescriptor field, StringBuilder builder) {
        Descriptors.FieldDescriptor.Type t = field.getType();
        if (t == Descriptors.FieldDescriptor.Type.MESSAGE) {
            buildSchema(field.getMessageType(), builder);
        } else if (t == Descriptors.FieldDescriptor.Type.ENUM) {
            builder.append("\"(");
            List<Descriptors.EnumValueDescriptor> values = field.getEnumType().getValues();
            for (int j = 0; j < values.size(); ++j) {
                if (j > 0) {
                    builder.append("|");
                }
                builder.append(values.get(j).getName());
            }
            builder.append(")\"");
        } else {
            builder.append('"').append(t).append('"');
        }
    }
}
