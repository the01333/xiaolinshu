package com.puxinxiaolin.xiaolinshu.auth.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * @Description: 把 AccessId 和 AccessKey 托管到 nacos 配置中心
 * @Author: YCcLin
 * @Date: 2025/10/1 16:25
 */
@ConfigurationProperties(prefix = "sdk.aliyun")
@Component
@Data
@RefreshScope
public class AliyunAccessKeyProperties {

    private String accessKeyId;

    private String accessKeySecret;

}
