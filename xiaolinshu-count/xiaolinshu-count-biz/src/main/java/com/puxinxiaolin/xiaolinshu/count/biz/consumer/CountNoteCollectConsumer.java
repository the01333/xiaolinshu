package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Maps;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.enums.CollectUnCollectNoteTypeEnum;
import com.puxinxiaolin.xiaolinshu.count.biz.enums.LikeUnlikeNoteTypeEnum;
import com.puxinxiaolin.xiaolinshu.count.biz.model.dto.CountCollectUnCollectNoteMqDTO;
import com.puxinxiaolin.xiaolinshu.count.biz.model.dto.CountLikeUnlikeNoteMqDTO;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT,
        topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT
)
public class CountNoteCollectConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000)  // 缓存队列的最大容量
            .batchSize(1000)   // 一批次最多聚合 1000 条
            .linger(Duration.ofSeconds(1))  // 多久聚合一次
            .setConsumerEx(this::consumeMessage)  // 设置消费者方法
            .build();

    @Override
    public void onMessage(String message) {
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(message);
    }

    /**
     * 聚合器回调的消费方法（批处理机制提高了并发能力和处理效率）
     *
     * @param messages
     */
    private void consumeMessage(List<String> messages) {
        
        log.info("==> 【笔记收藏数】聚合消息, size: {}", messages.size());
        log.info("==> 【笔记收藏数】聚合消息, {}", JsonUtils.toJsonString(messages));

        // 笔记收藏数据聚合处理
        List<CountCollectUnCollectNoteMqDTO> countCollectUnCollectNoteMqDTOS  = messages.stream()
                .map(message -> JsonUtils.parseObject(message, CountCollectUnCollectNoteMqDTO.class))
                .toList();

        Map<Long, List<CountCollectUnCollectNoteMqDTO>> groupMap = countCollectUnCollectNoteMqDTOS.stream()
                .collect(Collectors.groupingBy(CountCollectUnCollectNoteMqDTO::getNoteId));

        Map<Long, Integer> resultMap = Maps.newHashMap();
        for (Map.Entry<Long, List<CountCollectUnCollectNoteMqDTO>> entry : groupMap.entrySet()) {
            List<CountCollectUnCollectNoteMqDTO> list = entry.getValue();
            int finalCount = 0;

            for (CountCollectUnCollectNoteMqDTO mqDTO : list) {
                Integer type = mqDTO.getType();
                CollectUnCollectNoteTypeEnum collectUnCollectNoteTypeEnum = CollectUnCollectNoteTypeEnum.valueOf(type);
                if (Objects.isNull(collectUnCollectNoteTypeEnum)) {
                    continue;
                }
                
                switch (collectUnCollectNoteTypeEnum) {
                    case COLLECT -> finalCount += 1;
                    case UN_COLLECT -> finalCount -= 1;
                }
            }
            
            resultMap.put(entry.getKey(), finalCount);
        }
        
        log.info("## 聚合后的计数数据: {}", JsonUtils.toJsonString(resultMap));
        
        // 同步到 redis
        resultMap.forEach((noteId, count) -> {
            String redisKey = RedisKeyConstants.buildCountNoteKey(noteId);
            Boolean isExisted = redisTemplate.hasKey(redisKey);
            if (isExisted) {
                redisTemplate.opsForHash()
                        .increment(redisKey, RedisKeyConstants.FIELD_COLLECT_TOTAL, count);
            }
        });
        
        // 走 MQ, 数据入库
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(resultMap))
                .build();
     
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务: 笔记收藏数入库】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务: 笔记收藏数入库】MQ 发送异常: ", throwable);
            }
        });
    }

}
