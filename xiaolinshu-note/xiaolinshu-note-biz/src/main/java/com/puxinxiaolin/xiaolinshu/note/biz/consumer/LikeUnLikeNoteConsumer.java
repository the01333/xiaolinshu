package com.puxinxiaolin.xiaolinshu.note.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.note.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteLikeDO;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.NoteLikeDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.model.dto.LikeUnLikeNoteMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
@Slf4j
@RocketMQMessageListener(
        topic = MQConstants.TOPIC_LIKE_OR_UNLIKE,
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_LIKE_OR_UNLIKE,
        consumeMode = ConsumeMode.ORDERLY
)
public class LikeUnLikeNoteConsumer implements RocketMQListener<Message> {
    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private RateLimiter rateLimiter;

    @Override
    public void onMessage(Message message) {
        // 流量削峰: 获取到令牌才往下执行, 否则阻塞
        rateLimiter.acquire();

        // 幂等性: 通过联合唯一索引保证

        String bodyJson = new String(message.getBody());
        String tags = message.getTags();

        log.info("==> LikeUnlikeNoteConsumer 消费了消息 {}, tags: {}", bodyJson, tags);

        if (Objects.equals(tags, MQConstants.TAG_LIKE)) {
            handleLikeTagMessage(bodyJson);
        } else if (Objects.equals(tags, MQConstants.TAG_UNLIKE)) {
            handleUnlikeTagMessage(bodyJson);
        }
    }

    /**
     * 笔记取消点赞
     *
     * @param bodyJson
     */
    private void handleUnlikeTagMessage(String bodyJson) {
        LikeUnLikeNoteMqDTO mqDTO = JsonUtils.parseObject(bodyJson, LikeUnLikeNoteMqDTO.class);
        if (mqDTO == null) {
            return;
        }

        Long userId = mqDTO.getUserId();
        Long noteId = mqDTO.getNoteId();
        Integer type = mqDTO.getType();
        LocalDateTime createTime = mqDTO.getCreateTime();
        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
                .userId(userId)
                .noteId(noteId)
                .status(type)
                .createTime(createTime)
                .build();

        int count = noteLikeDOMapper.update2UnlikeByUserIdAndNoteId(noteLikeDO);
        if (count == 0) {
            return;
        }
        
        // 发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJson)
                .build();
        
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记取消点赞】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记取消点赞】MQ 发送异常: ", throwable);
            }
        });
    }

    /**
     * 笔记点赞
     *
     * @param bodyJson
     */
    private void handleLikeTagMessage(String bodyJson) {
        LikeUnLikeNoteMqDTO mqDTO = JsonUtils.parseObject(bodyJson, LikeUnLikeNoteMqDTO.class);
        if (mqDTO == null) {
            return;
        }

        Long userId = mqDTO.getUserId();
        Long noteId = mqDTO.getNoteId();
        Integer type = mqDTO.getType();
        LocalDateTime createTime = mqDTO.getCreateTime();
        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
                .userId(userId)
                .noteId(noteId)
                .status(type)
                .createTime(createTime)
                .build();

        int count = noteLikeDOMapper.insertOrUpdate(noteLikeDO);
        if (count == 0) {
            return;
        }

        // 发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJson)
                .build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记点赞】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记点赞】MQ 发送异常: ", throwable);
            }
        });
    }

}
