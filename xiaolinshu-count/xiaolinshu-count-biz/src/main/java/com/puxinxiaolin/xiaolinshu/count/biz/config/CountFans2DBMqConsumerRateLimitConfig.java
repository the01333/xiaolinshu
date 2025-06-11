package com.puxinxiaolin.xiaolinshu.count.biz.config;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RefreshScope
@Configuration
public class CountFans2DBMqConsumerRateLimitConfig {
    
    @Resource
    private CountFans2DBMqConsumerRateLimitProperties properties;
    
    @Bean
    @RefreshScope
    public RateLimiter rateLimiter() {
        return RateLimiter.create(properties.getRateLimit());
    }
    
}
