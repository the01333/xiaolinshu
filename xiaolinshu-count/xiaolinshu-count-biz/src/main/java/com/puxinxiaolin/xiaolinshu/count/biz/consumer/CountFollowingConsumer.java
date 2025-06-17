package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.enums.FollowUnfollowTypeEnum;
import com.puxinxiaolin.xiaolinshu.count.biz.model.dto.CountFollowUnfollowMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_COUNT_FOLLOWING,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING
)
public class CountFollowingConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    
    @Override
    public void onMessage(String body) {
        log.info("## 消费到了 MQ 【计数: 关注数】, {}...", body);

        // 关注数和粉丝数计数场景不同, 单个用户无法短时间内关注大量用户, 所以无需聚合
        // 直接对 Redis 中的 Hash 进行 +1 或 -1 操作即可
        CountFollowUnfollowMqDTO mqDTO = JsonUtils.parseObject(body, CountFollowUnfollowMqDTO.class);
        Integer type = mqDTO.getType();
        Long userId = mqDTO.getUserId();

        String key = RedisKeyConstants.buildCountUserKey(userId);
        Boolean isExisted = redisTemplate.hasKey(key);
        if (isExisted) {
            // 关注 +1, 取关 -1
            int count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;
            redisTemplate.opsForHash().increment(key, RedisKeyConstants.FIELD_FOLLOWING_TOTAL, count);
        }
        
        // 走 MQ
        Message<String> message = MessageBuilder.withPayload(body)
                .build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务: 关注数入库】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("==> 【计数服务: 关注数入库】MQ 发送异常: ", throwable);
            }
        });
    }
    
}
