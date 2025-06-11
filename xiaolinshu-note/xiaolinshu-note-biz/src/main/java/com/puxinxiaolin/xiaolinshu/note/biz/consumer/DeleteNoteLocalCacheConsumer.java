package com.puxinxiaolin.xiaolinshu.note.biz.consumer;

import com.puxinxiaolin.xiaolinshu.note.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.note.biz.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,
        topic = MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,
        messageModel = MessageModel.BROADCASTING
)
public class DeleteNoteLocalCacheConsumer implements RocketMQListener<String> {
    
    @Resource
    private NoteService noteService;
    
    @Override
    public void onMessage(String message) {
        Long noteId = Long.valueOf(message);
        log.info("## 消费者消费成功, noteId: {}", noteId);
        
        noteService.deleteNoteLocalCache(noteId);
    }
    
}
