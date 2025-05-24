package com.puxinxiaolin.xiaolinshu.oss.biz.factory;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.puxinxiaolin.xiaolinshu.oss.biz.strategy.FileStrategy;
import com.puxinxiaolin.xiaolinshu.oss.biz.strategy.impl.AliyunOssFileStrategy;
import com.puxinxiaolin.xiaolinshu.oss.biz.strategy.impl.MinioFileStrategy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description: 文件类型策略工厂
 * @Author: YCcLin
 * @Date: 2025/5/23 20:51
 */
@Configuration
@RefreshScope
public class FileStrategyFactory {

    @Value("${storage.type}")
    private String strategy;
    
    @Bean
    @RefreshScope
    public FileStrategy getFileStrategy() {
        if (StringUtils.equals(strategy, "aliyun")) {
            return new AliyunOssFileStrategy();
        } else if (StringUtils.equals(strategy, "minio")) {
            return new MinioFileStrategy();
        }

        throw new IllegalArgumentException("不可用的存储类型");
    }

}
