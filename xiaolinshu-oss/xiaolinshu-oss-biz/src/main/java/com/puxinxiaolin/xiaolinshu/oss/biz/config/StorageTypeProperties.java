package com.puxinxiaolin.xiaolinshu.oss.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "storage")
@Data
public class StorageTypeProperties {
    
    private String type;
    
}
