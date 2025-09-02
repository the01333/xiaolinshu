package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.domain.mapper.NoteCountDOMapper;
import com.puxinxiaolin.xiaolinshu.count.biz.model.dto.CountPublishCommentMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_COUNT_NOTE_COMMENT,
        topic = MQConstants.TOPIC_COUNT_NOTE_COMMENT
)
@Component
@Slf4j
public class CountNoteCommentConsumer implements RocketMQListener<String> {
    @Resource
    private NoteCountDOMapper noteCountDOMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000)
            .batchSize(1000)
            .linger(Duration.ofSeconds(1))
            .setConsumerEx(this::consumeMessage)
            .build();
    
    @Override
    public void onMessage(String message) {
        bufferTrigger.enqueue(message);
    }

    private void consumeMessage(List<String> messages) {
        log.info("==> 【笔记评论数】聚合消息, size: {}", messages.size());
        log.info("==> 【笔记评论数】聚合消息, {}", JsonUtils.toJsonString(messages));

        List<CountPublishCommentMqDTO> countPublishCommentMqDTOList = Lists.newArrayList();
        messages.forEach(message -> {
            try {
                CountPublishCommentMqDTO mqDTO = JsonUtils.parseObject(message, CountPublishCommentMqDTO.class);
                countPublishCommentMqDTOList.add(mqDTO);
            } catch (Exception e) {
                log.error("", e);
            }
        });

        Map<Long, List<CountPublishCommentMqDTO>> groupMap = countPublishCommentMqDTOList.stream()
                .collect(Collectors.groupingBy(CountPublishCommentMqDTO::getNoteId));
        for (Map.Entry<Long, List<CountPublishCommentMqDTO>> entry : groupMap.entrySet()) {
            Long noteId = entry.getKey();
            int count = CollUtil.size(entry.getValue());
            
            // 更新 redis 笔记评论数
            String hashKey = RedisKeyConstants.buildCountNoteKey(noteId);
            Boolean hasKey = redisTemplate.hasKey(hashKey);
            if (hasKey) {
                redisTemplate.opsForHash()
                        .increment(hashKey, RedisKeyConstants.FIELD_COMMENT_TOTAL, count);
            }

            if (count > 0) {
                noteCountDOMapper.insertOrUpdateCommentTotalByNoteId(count, noteId);
            }
        }
    }
    
}
