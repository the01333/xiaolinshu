package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.domain.mapper.UserCountDOMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_COUNT_FANS_2_DB,
        topic = MQConstants.TOPIC_COUNT_FANS_2_DB
)
public class CountFans2DBConsumer implements RocketMQListener<String> {
    
    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private UserCountDOMapper userCountDOMapper;
    
    @Override
    public void onMessage(String message) {
        // 流量削峰: 获取到令牌才往下执行, 否则阻塞
        rateLimiter.acquire();
        
        log.info("## 消费到了 MQ 【计数: 粉丝数入库】, {}...", message);

        Map<Long, Integer> countMap = null;
        try {
            countMap = JsonUtils.parseMap(message, Long.class, Integer.class);
        } catch (JsonProcessingException ex) {
            log.error("## 解析 JSON 字符串异常", ex);
        }
        
        if (CollUtil.isNotEmpty(countMap)) {
            countMap.forEach((userId, count) -> 
                    userCountDOMapper.insertOrUpdateFansTotalByUserId(count, userId)
            );
        }

    }
    
}
