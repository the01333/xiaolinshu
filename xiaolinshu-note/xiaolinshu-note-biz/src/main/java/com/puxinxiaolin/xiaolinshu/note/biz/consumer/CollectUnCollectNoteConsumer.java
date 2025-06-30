package com.puxinxiaolin.xiaolinshu.note.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.note.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteCollectionDO;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.model.dto.CollectUnCollectNoteMqDTO;
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

import java.util.Objects;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_COLLECT_OR_UN_COLLECT,
        topic = MQConstants.TOPIC_COLLECT_OR_UN_COLLECT,
        consumeMode = ConsumeMode.ORDERLY
)
public class CollectUnCollectNoteConsumer implements RocketMQListener<Message> {
    @Resource
    private NoteCollectionDOMapper noteCollectionDOMapper;
    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(Message message) {
        // 流量削峰: 获取到令牌才往下执行, 否则阻塞
        rateLimiter.acquire();

        // 幂等性: 通过联合唯一索引保证

        String body = new String(message.getBody());
        String tags = message.getTags();

        log.info("==> CollectUnCollectNoteConsumer 消费了消息 {}, tags: {}",
                body, tags);

        if (Objects.equals(tags, MQConstants.TAG_COLLECT)) {
            handleCollectNoteTagMessage(body);
        } else {
            handleUnCollectNoteTagMessage(body);
        }
    }

    /**
     * 处理取消收藏笔记的 tag 消息
     *
     * @param body
     */ 
    private void handleUnCollectNoteTagMessage(String body) {
        CollectUnCollectNoteMqDTO mqDTO = JsonUtils.parseObject(body, CollectUnCollectNoteMqDTO.class);
        if (Objects.isNull(mqDTO)) {
            return;
        }

        NoteCollectionDO noteCollectionDO = NoteCollectionDO.builder()
                .userId(mqDTO.getUserId())
                .noteId(mqDTO.getNoteId())
                .status(mqDTO.getType())
                .createTime(mqDTO.getCreateTime())
                .build();

        int count = noteCollectionDOMapper.update2UnCollectByUserIdAndNoteId(noteCollectionDO);
        if (count == 0) {
            return;
        }
        
        // 发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(body)
                .build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记取消收藏】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记取消收藏】MQ 发送异常: ", throwable);
            }
        });
    }

    /**
     * 处理收藏笔记的 tag 消息
     *
     * @param body
     */
    private void handleCollectNoteTagMessage(String body) {
        CollectUnCollectNoteMqDTO mqDTO = JsonUtils.parseObject(body, CollectUnCollectNoteMqDTO.class);
        if (Objects.isNull(mqDTO)) {
            return;
        }

        NoteCollectionDO noteCollectionDO = NoteCollectionDO.builder()
                .userId(mqDTO.getUserId())
                .noteId(mqDTO.getNoteId())
                .status(mqDTO.getType())
                .createTime(mqDTO.getCreateTime())
                .build();
        int count = noteCollectionDOMapper.insertOrUpdate(noteCollectionDO);
        if (count == 0) {
            return;
        }

        // 发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(body))
                .build();
        
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记收藏】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记收藏】MQ 发送异常: ", throwable);
            }
        });
    }

}
