package com.puxinxiaolin.xiaolinshu.search.biz.config;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

/**
 * @Description: 使用 canal 构建笔记、用户实时增量 es 索引
 * @Author: YCcLin
 * @Date: 2025/7/26 11:21
 */
@Configuration
public class CanalClient implements DisposableBean {

    @Resource
    private CanalProperties canalProperties;

    private CanalConnector canalConnector;

    @Bean
    public CanalConnector getCanalConnector() {
        String address = canalProperties.getAddress();
        String[] addressArr = address.split(":");
        String host = addressArr[0];
        int port = Integer.parseInt(addressArr[1]);

        canalConnector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(host, port),
                canalProperties.getDestination(),
                canalProperties.getUsername(),
                canalProperties.getPassword()
        );

        // 连接到 Canal 服务端
        canalConnector.connect();
        // 订阅 Canal 中的数据变化, 指定要监听的数据库和表（可以使用表名、数据库名的通配符）
        canalConnector.subscribe(canalProperties.getSubscribe());
        // 回滚 Canal 消费者的位点, 回滚到上次提交的消费位置
        canalConnector.rollback();

        return canalConnector;
    }

    @Override
    public void destroy() throws Exception {
        canalConnector.disconnect();
    }

}
