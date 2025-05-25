package com.puxinxiaolin.xiaolinshu.oss.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "oss.aliyun")
@Component
@Data
public class AliyunOSSProperties {
    
    private String endpoint;
    
    private String accessKey;
    
    private String secretKey;
    
}