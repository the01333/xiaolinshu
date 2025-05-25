package com.puxinxiaolin.xiaolinshu.oss.api.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description: 让 Feign 支持 form 表单提交
 * @Author: YCcLin
 * @Date: 2025/5/25 16:04
 */
@Configuration
public class FeignFormConfig {

    @Bean
    public Encoder feignFormEncoder() {
        return new SpringFormEncoder();
    }

}
