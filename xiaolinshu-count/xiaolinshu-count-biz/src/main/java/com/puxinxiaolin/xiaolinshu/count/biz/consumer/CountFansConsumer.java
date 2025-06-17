package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Maps;
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_COUNT_FANS,
        topic = MQConstants.TOPIC_COUNT_FANS
)
public class CountFansConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(5000)   // 缓存队列的最大容量
            .batchSize(1000)   // 一批次最多聚合多少条数据
            .linger(Duration.ofSeconds(1))   // 多久聚合一次
            .setConsumerEx(this::consumeMessage)   // 聚合成功后的处理
            .build();

    @Override
    public void onMessage(String message) {
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(message);
    }

    /**
     * 根据聚合结果消费消息:
     * 这里是要对一个用户的粉丝数做聚合, 因为可能有很多用户同时关注或者取关该用户,
     * 所以可以用 BufferTrigger 的流量聚合来优化
     *
     * @param messages
     */
    private void consumeMessage(List<String> messages) {
        log.info("==> 聚合消息, size: {}", messages.size());
        log.info("==> 聚合消息, {}", JsonUtils.toJsonString(messages));

        List<CountFollowUnfollowMqDTO> mqDTOList = messages.stream()
                .map(message -> JsonUtils.parseObject(message, CountFollowUnfollowMqDTO.class))
                .toList();

        // 按目标用户分组
        Map<Long, List<CountFollowUnfollowMqDTO>> groupMap = mqDTOList.stream()
                .collect(Collectors.groupingBy(CountFollowUnfollowMqDTO::getTargetUserId));

        // 按组汇总 <目标用户ID, 计数>
        Map<Long, Integer> countMap = Maps.newHashMap();
        for (Map.Entry<Long, List<CountFollowUnfollowMqDTO>> entry : groupMap.entrySet()) {
            List<CountFollowUnfollowMqDTO> list = entry.getValue();

            int finalCount = 0;
            for (CountFollowUnfollowMqDTO mqDTO : list) {
                Integer type = mqDTO.getType();
                FollowUnfollowTypeEnum followUnfollowTypeEnum = FollowUnfollowTypeEnum.valueOf(type);
                if (followUnfollowTypeEnum == null) {
                    continue;
                }

                switch (followUnfollowTypeEnum) {
                    case FOLLOW -> finalCount += 1;
                    case UNFOLLOW -> finalCount -= 1;
                }
            }
            countMap.put(entry.getKey(), finalCount);
        }

        log.info("## 聚合后的计数数据: {}", JsonUtils.toJsonString(countMap));

        // 走 redis
        countMap.forEach((k, v) -> {
            String key = RedisKeyConstants.buildCountUserKey(k);
            Boolean isExisted = redisTemplate.hasKey(key);
            if (isExisted) {
                redisTemplate.opsForHash()
                        .increment(key, RedisKeyConstants.FIELD_FANS_TOTAL, v);
            }
        });

        // 走 MQ, 确保数据落库
        /*
            存库这里, 单独再走一个 MQ, 需要考虑即使进行了聚合, 流量削峰了, 
            数据库可能仍承受不住, 独立一个消费者出来处理, 可以单独用令牌桶削峰, 
            进一步对流量进行控制, 防止打垮数据库
         */
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countMap))
                .build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务: 粉丝数入库】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务: 粉丝数入库】MQ 发送异常: ", throwable);
            }
        });
    }
}
