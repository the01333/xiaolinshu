package com.puxinxiaolin.xiaolinshu.count.biz.config;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
@RefreshScope
@Configuration
public class CountFans2DBMqConsumerRateLimitConfig {
    
    @Resource
    private CountFans2DBMqConsumerRateLimitProperties properties;

    /**
     * keypoint: 通过监听输出日志观察热更新后的值
     *
     * @param event
     */
    @EventListener
    public void handleRefreshEvent(RefreshScopeRefreshedEvent event) {
        log.info("## module:count -> CountFans2DBMqConsumerRateLimitProperties 的配置刷新后，当前令牌桶的大小为: {}", properties.getRateLimit());
    }
    
    @Bean
    @RefreshScope
    public RateLimiter rateLimiter() {
        return RateLimiter.create(properties.getRateLimit());
    }
    
}
