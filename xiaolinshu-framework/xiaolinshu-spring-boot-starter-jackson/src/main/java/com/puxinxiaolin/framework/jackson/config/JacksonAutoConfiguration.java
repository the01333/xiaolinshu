package com.puxinxiaolin.framework.jackson.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.YearMonthSerializer;
import com.puxinxiaolin.framework.common.constant.DateConstants;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@AutoConfiguration
public class JacksonAutoConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 忽略未知属性
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 设置凡是为 null 的字段，返参中均不返回，根据项目组约定是否开启
//        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // 支持 LocalDateTime、LocalDate、LocalTime
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateConstants.Y_M_D_H_M_S));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateConstants.Y_M_D_H_M_S));
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateConstants.Y_M_D));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateConstants.Y_M_D));
        javaTimeModule.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateConstants.H_M_S));
        javaTimeModule.addDeserializer(LocalTime.class,
                new LocalTimeDeserializer(DateConstants.H_M_S));

        // 支持 YearMonth
        javaTimeModule.addSerializer(YearMonth.class,
                new YearMonthSerializer(DateConstants.Y_M));
        javaTimeModule.addDeserializer(YearMonth.class,
                new YearMonthDeserializer(DateConstants.Y_M));

        objectMapper.registerModule(javaTimeModule);

        // 初始化 JsonUtils 中的 ObjectMapper
        JsonUtils.init(objectMapper);

        return objectMapper;
    }
    
}
