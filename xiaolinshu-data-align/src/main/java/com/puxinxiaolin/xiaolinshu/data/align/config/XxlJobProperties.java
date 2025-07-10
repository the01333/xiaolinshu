package com.puxinxiaolin.xiaolinshu.data.align.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = XxlJobProperties.PREFIX)
@Component
@Data
public class XxlJobProperties {
    
    public static final String PREFIX = "xxl.job";

    public String adminAddresses;
    
    public String accessToken;
    
    public String appName;
    
    public String ip;
    
    public int port;
    
    public String logPath;
    
    public int logRetentionDays = 30;
    
}
