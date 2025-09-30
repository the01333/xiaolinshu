package com.puxinxiaolin.xiaolinshu.count.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * @Description: 把令牌桶的每秒限流阈值交由 nacos 动态管理
 * @Author: YCcLin
 * @Date: 2025/6/11 18:59
 */
@Component
@Data
@ConfigurationProperties(prefix = "mq-consumer.follow-unfollow")
@RefreshScope
public class CountFans2DBMqConsumerRateLimitProperties {

    private double rateLimit;
    
}
