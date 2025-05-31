package com.puxinxiaolin.xiaolinshu.note.biz.config;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @Description: 解决兼容问题
 * 由于 rocketmq 依赖本身是基于 SpringBoot 2.x 开发的,
 * 2.x 和 3.x 在定义 starter 时的格式是有区别的, 所以这里手动引入一下自动配置类
 * @Author: YCcLin
 * @Date: 2025/5/31 11:53
 */
@Configuration
@Import(RocketMQAutoConfiguration.class)
public class RocketMQConfig {
    
}
