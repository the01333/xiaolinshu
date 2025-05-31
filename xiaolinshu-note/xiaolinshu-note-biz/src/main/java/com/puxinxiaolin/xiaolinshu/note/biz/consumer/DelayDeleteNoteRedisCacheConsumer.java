package com.puxinxiaolin.xiaolinshu.note.biz.consumer;

import com.puxinxiaolin.xiaolinshu.note.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.note.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.note.biz.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE,
        topic = MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE
)
public class DelayDeleteNoteRedisCacheConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(String message) {
        Long noteId = Long.valueOf(message);
        log.info("## 延迟消息消费成功, noteId: {}", noteId);

        // 删除笔记缓存
        String key = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(key);
    }

}
