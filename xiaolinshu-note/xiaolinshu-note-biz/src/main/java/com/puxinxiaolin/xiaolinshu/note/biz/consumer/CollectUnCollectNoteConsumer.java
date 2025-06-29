package com.puxinxiaolin.xiaolinshu.note.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.note.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteCollectionDO;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.model.dto.CollectUnCollectNoteMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
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
    private RateLimiter rateLimiter;
    @Resource
    private NoteCollectionDOMapper noteCollectionDOMapper;

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
     * 处理收藏笔记的 tag 消息
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

        // TODO [YCcLin 2025/6/29]: 发送计数 MQ 
    }

    /**
     * 处理取消收藏笔记的 tag 消息
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

        // TODO [YCcLin 2025/6/29]: 发送计数 MQ 
    }

}
