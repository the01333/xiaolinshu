package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import com.puxinxiaolin.xiaolinshu.count.biz.constant.MQConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_COUNT_FOLLOWING,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING
)
public class CountFollowingConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("## 消费到了 MQ 【计数: 关注数】, {}...", message);
    }
    
}
