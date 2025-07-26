package com.puxinxiaolin.xiaolinshu.comment.biz.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * @Description: 自定义重试配置, 使用 template 模板方法来替代 Spring Retry 框架的注解方式
 * @Author: YCcLin
 * @Date: 2025/7/26 19:55
 */
@Configuration
public class RetryConfig {

    @Resource
    private RetryProperties retryProperties;

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 定义重试策略（最多重试 3 次）
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(retryProperties.getMaxAttempts());

        // 定义间隔策略
        ExponentialBackOffPolicy backoffPolicy = new ExponentialBackOffPolicy();
        backoffPolicy.setInitialInterval(retryProperties.getInitInterval());
        backoffPolicy.setMultiplier(retryProperties.getMultiplier());

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backoffPolicy);

        return retryTemplate;
    }

}
