package com.puxinxiaolin.framework.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonUtils {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将 JSON 字符串转换为 Set
     *
     * @param jsonStr
     * @param clazz
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> Set<T> parseSet(String jsonStr, Class<T> clazz) throws Exception {
        return OBJECT_MAPPER.readValue(jsonStr, new TypeReference<Set<T>>() {
            @Override
            public Type getType() {
                return OBJECT_MAPPER.getTypeFactory().constructCollectionType(Set.class, clazz);
            }
        });
    }

    /**
     * 将 JSON 字符串转换为 List
     *
     * @param jsonStr
     * @param clazz
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> List<T> parseList(String jsonStr, Class<T> clazz) throws Exception {

        return OBJECT_MAPPER.readValue(jsonStr, new TypeReference<List<T>>() {
            @Override
            public Type getType() {
                return OBJECT_MAPPER.getTypeFactory()
                        .constructCollectionType(List.class, clazz);
            }
        });
    }

    /**
     * 将 JSON 字符串转换为 Map
     *
     * @param jsonStr
     * @param keyClass
     * @param valueClass
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<K, V> parseMap(String jsonStr, Class<K> keyClass, Class<V> valueClass) throws JsonProcessingException {
        TypeReference<Map<K, V>> typeRef = new TypeReference<>() {
        };

        return OBJECT_MAPPER.readValue(
                jsonStr,
                OBJECT_MAPPER.getTypeFactory()
                        .constructMapType(Map.class, keyClass, valueClass)
        );
    }

    /**
     * 将对象转换为 JSON 字符串
     *
     * @param obj
     * @return
     */
    @SneakyThrows
    public static String toJsonString(Object obj) {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    /**
     * 将 JSON 字符串转换为对象
     *
     * @param jsonStr
     * @param clazz
     * @param <T>
     * @return
     */
    @SneakyThrows
    public static <T> T parseObject(String jsonStr, Class<T> clazz) {
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }

        return OBJECT_MAPPER.readValue(jsonStr, clazz);
    }

    /**
     * 初始化：统一使用 Spring Boot 个性化配置的 ObjectMapper
     *
     * @param objectMapper
     */
    public static void init(ObjectMapper objectMapper) {
        OBJECT_MAPPER = objectMapper;
    }

}
