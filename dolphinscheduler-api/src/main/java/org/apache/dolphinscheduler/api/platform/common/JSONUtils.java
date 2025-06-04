package org.apache.dolphinscheduler.api.platform.common;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JSONUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * <br>
     * 得到实例
     *
     * @return
     */
    public static ObjectMapper getInstance() {
        return OBJECT_MAPPER;
    }

    /**
     * 将Object对象转为JSON字符串
     *
     * @param object
     * @return
     */
    public static String toJson(Object object) {
        String json = null;
        try {
            json = OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("To json error, object is " + object, e);
        }
        return json;
    }

    /**
     * 将一个JSON字符串转换为Object对象
     *
     * @param <T>
     * @param json
     * @param clazz
     * @return
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        T o = null;
        if (StringUtils.isNotBlank(json)) {
            try {
                o = OBJECT_MAPPER.readValue(json, clazz);
            } catch (Exception e) {
                throw new RuntimeException("Json string To object error, json is " + json, e);
            }
        }
        return o;
    }

    /**
     * 将一个输入流转换为Object对象
     *
     * @param <T>
     * @param input
     * @param clazz
     * @return
     */
    public static <T> T toObject(InputStream input, Class<T> clazz) {
        T o = null;
        if (input != null) {
            try {
                o = OBJECT_MAPPER.readValue(input, clazz);
            } catch (Exception e) {
                throw new RuntimeException("inputstream To object error, ", e);
            }
        }
        return o;
    }

    /**
     * 将一个JSON字符串转换为Object对象
     *
     * @param <T>
     * @param json
     * @param javaType
     * @return
     */
    public static <T> T toObject(String json, JavaType javaType) {
        T o = null;
        if (StringUtils.isNotBlank(json)) {
            try {
                o = OBJECT_MAPPER.readValue(json, javaType);
            } catch (Exception e) {
                throw new RuntimeException("Json string To object error, json is " + json, e);
            }
        }
        return o;
    }

    /**
     * 将一个JSON字符串转换为Object对象
     *
     * @param <T>
     * @param input
     * @param javaType
     * @return
     */
    public static <T> T toObject(InputStream input, JavaType javaType) {
        T o = null;
        if (input != null) {
            try {
                o = OBJECT_MAPPER.readValue(input, javaType);
            } catch (Exception e) {
                throw new RuntimeException("InputStream To object error ", e);
            }
        }
        return o;
    }

    /**
     * 将一个JSON字符串转换为T对象
     *
     * @param json
     * @param typeReference
     * @return
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("failed to convert string " + json + " to object", e);
        }
    }

    public static void registerModule(com.fasterxml.jackson.databind.Module moudle) {
        OBJECT_MAPPER.registerModule(moudle);
    }

    public static String compress(String json) {
        try {
            if (json == null || json.length() == 0) {
                return OBJECT_MAPPER.readTree("{}").toString();
            }
            return OBJECT_MAPPER.readTree(json).toString();
        } catch (IOException e) {
            throw new RuntimeException("failed to compress string " + json, e);
        }
    }
}
