package com.puxinxiaolin.xiaolinshu.note.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Description: 把令牌桶的每秒限流阈值交由 nacos 动态管理
 * @Author: YCcLin
 * @Date: 2025/6/22 16:31
 */
@Data
@Component
@ConfigurationProperties(prefix = "mq-consumer.like-unlike")
public class LikeUnLikeNoteMqConsumerRateLimitProperties {

    private double rateLimit;
    
}
