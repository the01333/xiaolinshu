package com.puxinxiaolin.xiaolinshu.note.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.note.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteLikeDO;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.NoteLikeDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.model.dto.LikeUnLikeNoteMqDTO;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LikeUnLikeNoteConsumer {
    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;
    @Resource
    private RateLimiter rateLimiter;

    private DefaultMQPushConsumer consumer;

    @Value("${rocketmq.name-server}")
    private String namesrvAddr;

    @Bean(name = "LikeUnlikeNoteConsumer")
    public DefaultMQPushConsumer defaultMQPushConsumer() throws MQClientException {
        String group = "xiaolinshu_group_" + MQConstants.TOPIC_LIKE_OR_UNLIKE;

        consumer = new DefaultMQPushConsumer();

        consumer.setNamesrvAddr(namesrvAddr);
        consumer.subscribe(MQConstants.TOPIC_LIKE_OR_UNLIKE, "*");
        consumer.setMessageModel(MessageModel.CLUSTERING);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setMaxReconsumeTimes(3);
        consumer.setConsumeMessageBatchMaxSize(30);
        consumer.setPullInterval(1000);

        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            log.info("==> 【笔记点赞、取消点赞】本批次消息大小: {}", msgs.size());

            try {
                // 令牌桶限流
                rateLimiter.acquire();

                // 幂等性: 通过联合唯一索引保证
                List<LikeUnLikeNoteMqDTO> likeUnLikeNoteMqDTOS = Lists.newArrayList();
                msgs.forEach(msg -> {
                    String message = new String(msg.getBody());
                    
                    log.info("==> Consumer - Received message: {}", message);
                    
                    likeUnLikeNoteMqDTOS.add(JsonUtils.parseObject(message, LikeUnLikeNoteMqDTO.class));
                });

                // 1. 内存级操作合并
                // 按用户 ID 进行分组
                Map<Long, List<LikeUnLikeNoteMqDTO>> groupMap = likeUnLikeNoteMqDTOS.stream()
                        .collect(Collectors.groupingBy(LikeUnLikeNoteMqDTO::getUserId));
                // 对每个用户的操作按 noteId 二次分组, 并过滤合并
                List<LikeUnLikeNoteMqDTO> finalOperations = groupMap.values().stream()
                        .flatMap(userOperations -> {
                            // Map<noteId, List<MqDTO>>
                            Map<Long, List<LikeUnLikeNoteMqDTO>> noteGroupMap = userOperations.stream()
                                    .collect(Collectors.groupingBy(LikeUnLikeNoteMqDTO::getNoteId));

                            return noteGroupMap.values().stream()
                                    .filter(operations -> {
                                        int size = operations.size();
                                        // 偶数次操作, 最终状态抵消, 无需写入; 奇数次操作, 保留最后一次操作
                                        return size % 2 != 0;
                                    }).map(ops -> ops.get(ops.size() - 1));
                        }).toList();

                // 2. 批量入库
                if (CollUtil.isNotEmpty(finalOperations)) {
                    List<NoteLikeDO> noteLikeDOS = finalOperations.stream()
                            .map(dto -> NoteLikeDO.builder()
                                    .noteId(dto.getNoteId())
                                    .userId(dto.getUserId())
                                    .createTime(dto.getCreateTime())
                                    .status(dto.getType())
                                    .build()
                            ).toList();
                    
                    noteLikeDOMapper.batchInsertOrUpdate(noteLikeDOS);
                }

                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Exception e) {
                log.error("", e);
                // 这样 RocketMQ 会暂停当前队列的消费一段时间, 再重试
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        });

        consumer.start();
        return consumer;
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

}
