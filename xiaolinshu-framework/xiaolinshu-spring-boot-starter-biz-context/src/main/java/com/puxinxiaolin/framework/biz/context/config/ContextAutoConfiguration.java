package com.puxinxiaolin.framework.biz.context.config;

import com.puxinxiaolin.framework.biz.context.filter.HeaderUserId2ContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ContextAutoConfiguration {

    /**
     * 用 FilterRegistrationBean 注册过滤器
     *
     * @return
     */
    @Bean
    public FilterRegistrationBean<HeaderUserId2ContextFilter> filterFilterRegistrationBean() {
        return new FilterRegistrationBean<>(new HeaderUserId2ContextFilter());
    }

}
