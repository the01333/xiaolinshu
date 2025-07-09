package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.domain.mapper.UserCountDOMapper;
import com.puxinxiaolin.xiaolinshu.count.biz.model.dto.NoteOperateMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_NOTE_OPERATE,
        topic = MQConstants.TOPIC_NOTE_OPERATE
)
public class CountNotePublishConsumer implements RocketMQListener<Message> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserCountDOMapper userCountDOMapper;

    @Override
    public void onMessage(Message message) {
        String bodyStr = new String(message.getBody());
        String tags = message.getTags();

        log.info("==> CountNotePublishConsumer 消费了消息: {}, tags: {}", bodyStr, tags);

        if (Objects.equals(tags, MQConstants.TAG_NOTE_PUBLISH)) {
            handleTagMessage(bodyStr, 1);
        } else {
            handleTagMessage(bodyStr, -1);
        }

    }

    /**
     * 笔记发布、删除
     *
     * @param bodyStr
     */
    private void handleTagMessage(String bodyStr, Integer count) {
        NoteOperateMqDTO mqDTO = JsonUtils.parseObject(bodyStr, NoteOperateMqDTO.class);
        if (Objects.isNull(mqDTO)) {
            return;
        }

        Long creatorId = mqDTO.getCreatorId();
        String redisKey = RedisKeyConstants.buildCountUserKey(creatorId);
        Boolean isExisted = redisTemplate.hasKey(redisKey);
        if (isExisted) {
            redisTemplate.opsForHash()
                    .increment(redisKey, RedisKeyConstants.FIELD_NOTE_TOTAL, count);
        }

        userCountDOMapper.insertOrUpdateNoteTotalByUserId(count, creatorId);
    }

}
