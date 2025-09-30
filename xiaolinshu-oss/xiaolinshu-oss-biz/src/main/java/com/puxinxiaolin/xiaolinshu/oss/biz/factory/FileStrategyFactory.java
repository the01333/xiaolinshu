package com.puxinxiaolin.xiaolinshu.oss.biz.factory;

import com.puxinxiaolin.xiaolinshu.oss.biz.config.StorageTypeProperties;
import com.puxinxiaolin.xiaolinshu.oss.biz.strategy.FileStrategy;
import com.puxinxiaolin.xiaolinshu.oss.biz.strategy.impl.AliyunOssFileStrategy;
import com.puxinxiaolin.xiaolinshu.oss.biz.strategy.impl.MinioFileStrategy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * @Description: 文件类型策略工厂
 * @Author: YCcLin
 * @Date: 2025/5/23 20:51
 */
@Slf4j
@Configuration
@RefreshScope
public class FileStrategyFactory {

    @Resource
    private StorageTypeProperties storageTypeProperties;

    /**
     * keypoint: 通过监听输出日志观察热更新后的值
     *
     * @param event
     */
    @EventListener
    public void handleRefreshEvent(RefreshScopeRefreshedEvent event) {
        log.info("## 配置刷新后，当前文件存储的平台为: {}", storageTypeProperties.getType());
    }

    @Bean
    @RefreshScope
    public FileStrategy getFileStrategy() {
        String strategy = storageTypeProperties.getType();
        log.info("## 当前文件存储的平台为: {}", strategy);

        if (StringUtils.equals(strategy, "aliyun")) {
            return new AliyunOssFileStrategy();
        } else if (StringUtils.equals(strategy, "minio")) {
            return new MinioFileStrategy();
        }

        throw new IllegalArgumentException("不可用的存储类型");
    }

}
