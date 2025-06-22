package com.puxinxiaolin.xiaolinshu.note.biz.config;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description: 令牌桶的令牌数量托管到 nacos 配置中心
 * @Author: YCcLin
 * @Date: 2025/6/22 16:33
 */
@RefreshScope
@Configuration
public class LikeUnLikeNoteMqConsumerRateLimitConfig {

    @Resource
    private LikeUnLikeNoteMqConsumerRateLimitProperties properties;
    
    @Bean
    @RefreshScope
    public RateLimiter rateLimiter() {
        return RateLimiter.create(properties.getRateLimit());
    }

}
