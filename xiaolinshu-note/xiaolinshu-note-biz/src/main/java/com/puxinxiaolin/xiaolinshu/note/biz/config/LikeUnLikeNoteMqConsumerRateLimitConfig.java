package com.puxinxiaolin.xiaolinshu.note.biz.config;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * @Description: 令牌桶的令牌数量托管到 nacos 配置中心
 * @Author: YCcLin
 * @Date: 2025/6/22 16:33
 */
@RefreshScope
@Configuration
@Slf4j
public class LikeUnLikeNoteMqConsumerRateLimitConfig {

    @Resource
    private LikeUnLikeNoteMqConsumerRateLimitProperties properties;

    /**
     * keypoint: 通过监听输出日志观察热更新后的值
     *
     * @param event
     */
    @EventListener
    public void handleRefreshEvent(RefreshScopeRefreshedEvent event) {
        log.info("## module:note -> LikeUnLikeNoteMqConsumerRateLimitProperties 的配置刷新后，当前令牌桶的大小为: {}", properties.getRateLimit());
    }
    
    @Bean
    @RefreshScope
    public RateLimiter rateLimiter() {
        return RateLimiter.create(properties.getRateLimit());
    }

}
