package com.puxinxiaolin.xiaolinshu.search.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = CanalProperties.PREFIX)
public class CanalProperties {
    
    public static final String PREFIX = "canal";
    
    private String address;
    
    private String destination;
    
    private String username;
    
    private String password;

    /**
     * 订阅规则
     */
    private String subscribe;

    /**
     * 一批次拉取数据量, 默认 1000 条
     */
    private Integer batchSize;
    
}
