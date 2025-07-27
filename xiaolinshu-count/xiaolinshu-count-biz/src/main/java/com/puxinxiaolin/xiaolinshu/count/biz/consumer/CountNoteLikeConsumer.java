package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.enums.LikeUnlikeNoteTypeEnum;
import com.puxinxiaolin.xiaolinshu.count.biz.model.dto.AggregationCountLikeUnLikeNoteMqDTO;
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
import java.util.stream.Collectors;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_LIKE_OR_UNLIKE,
        topic = MQConstants.TOPIC_LIKE_OR_UNLIKE
)
public class CountNoteLikeConsumer implements RocketMQListener<String> {
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

        log.info("==> 【笔记点赞数】聚合消息, size: {}", messages.size());
        log.info("==> 【笔记点赞数】聚合消息, {}", JsonUtils.toJsonString(messages));

        List<CountLikeUnlikeNoteMqDTO> countLikeUnlikeNoteMqDTOS = messages.stream()
                .map(body -> JsonUtils.parseObject(body, CountLikeUnlikeNoteMqDTO.class))
                .toList();
        // 按笔记 ID 分组
        Map<Long, List<CountLikeUnlikeNoteMqDTO>> groupMap = countLikeUnlikeNoteMqDTOS.stream()
                .collect(Collectors.groupingBy(CountLikeUnlikeNoteMqDTO::getNoteId));

        // 按组汇总数据, 统计出最终的计数
        // key 为笔记 ID, value 为最终操作的计数
//        Map<Long, Integer> result = Maps.newHashMap();
        List<AggregationCountLikeUnLikeNoteMqDTO> countList = Lists.newArrayList();

        for (Map.Entry<Long, List<CountLikeUnlikeNoteMqDTO>> entry : groupMap.entrySet()) {
            Long noteId = entry.getKey();
            Long creatorId = null;
            List<CountLikeUnlikeNoteMqDTO> list = entry.getValue();

            int finalCount = 0;
            for (CountLikeUnlikeNoteMqDTO mqDTO : list) {
                creatorId = mqDTO.getNoteCreatorId();
                Integer type = mqDTO.getType();
                LikeUnlikeNoteTypeEnum typeEnum = LikeUnlikeNoteTypeEnum.valueOf(type);
                if (typeEnum == null) {
                    return;
                }

                switch (typeEnum) {
                    case LIKE -> finalCount += 1;
                    case UNLIKE -> finalCount -= 1;
                }
            }

            countList.add(AggregationCountLikeUnLikeNoteMqDTO.builder()
                    .noteId(noteId)
                    .creatorId(creatorId)
                    .count(finalCount)
                    .build());
        }

        log.info("## 【笔记点赞数】聚合后的计数数据: {}", JsonUtils.toJsonString(countList));

        // 同步 redis
        countList.forEach(item -> {
            Integer count = item.getCount();
            Long creatorId = item.getCreatorId();
            Long noteId = item.getNoteId();

            // 更新 redis 笔记维度点赞数
            String countNoteKey = RedisKeyConstants.buildCountNoteKey(noteId);
            Boolean isCountNoteExisted = redisTemplate.hasKey(countNoteKey);
            if (isCountNoteExisted) {
                redisTemplate.opsForHash()
                        .increment(countNoteKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
            }

            // 更新 redis 用户维度点赞数
            String countUserKey = RedisKeyConstants.buildCountUserKey(creatorId);
            Boolean isCountUserExisted = redisTemplate.hasKey(countUserKey);
            if (isCountUserExisted) {
                redisTemplate.opsForHash()
                        .increment(countUserKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
            }
        });
        
        // 走 MQ, 确保数据入库
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countList))
                .build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务: 笔记点赞数入库】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务: 笔记点赞数入库】MQ 发送异常: ", throwable);
            }
        });
    }

}
