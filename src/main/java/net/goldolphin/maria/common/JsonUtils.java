package net.goldolphin.maria.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by caofuxiang on 2017/3/17.
 */
public class JsonUtils {
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static final ObjectMapper JSON_MAPPER;

    static {
        JSON_MAPPER = new ObjectMapper(JSON_FACTORY);
        JSON_MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        JSON_MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    }

    public static String write(Object obj) throws IOException {
        return JSON_MAPPER.writeValueAsString(obj);
    }

    public static <T> void write(OutputStream out, T obj) throws IOException {
        JSON_MAPPER.writeValue(out, obj);
    }

    public static <T> T read(String input, Class<T> clazz) throws IOException {
        return JSON_MAPPER.readValue(input, clazz);
    }

    public static <T> T read(InputStream input, Class<T> clazz) throws IOException {
        return JSON_MAPPER.readValue(input, clazz);
    }

    public static JsonFactory factory() {
        return JSON_FACTORY;
    }

    public static JsonNodeFactory nodeFactory() {
        return JsonNodeFactory.instance;
    }

    public static String mergeObject(String defValue, String updateValue) throws IOException {
        if (defValue == null || defValue.length() == 0) {
            return updateValue;
        }
        if (updateValue == null || updateValue.length() == 0) {
            return defValue;
        }
        return write(read(defValue, ObjectNode.class).setAll(read(updateValue, ObjectNode.class)));
    }
}
