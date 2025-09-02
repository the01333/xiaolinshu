package com.puxinxiaolin.xiaolinshu.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject.CommentDO;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.enums.CommentLevelEnum;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.CountPublishCommentMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RocketMQMessageListener(
        consumerGroup = "xiaohashu_group_first_reply_comment_id_" + MQConstants.TOPIC_COUNT_NOTE_COMMENT,
        topic = MQConstants.TOPIC_COUNT_NOTE_COMMENT
)
@Component
@Slf4j
public class OneLevelCommentFirstReplyCommentIdUpdateConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

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
        log.info("==> 【一级评论 first_reply_comment_id 更新】聚合消息, size: {}", messages.size());
        log.info("==> 【一级评论 first_reply_comment_id 更新】聚合消息, {}", JsonUtils.toJsonString(messages));

        List<CountPublishCommentMqDTO> mqDTOList = Lists.newArrayList();
        messages.forEach(msg -> {
            try {
                List<CountPublishCommentMqDTO> dtos = JsonUtils.parseList(msg, CountPublishCommentMqDTO.class);

                mqDTOList.addAll(dtos);
            } catch (Exception e) {
                log.error("", e);
            }
        });

        // 过滤出二级评论的 parent_id, 去重后更新一级评论的 first_reply_comment_id
        List<Long> parentIds = mqDTOList.stream()
                .filter(dto -> Objects.equals(dto.getLevel(), CommentLevelEnum.TWO.getCode()))
                .map(CountPublishCommentMqDTO::getParentId)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(parentIds)) return;

        List<String> keys = parentIds.stream()
                .map(RedisKeyConstants::buildHaveFirstReplyCommentKey)
                .toList();
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);

        // 提取 Redis 中不存在的评论 ID
        List<Long> missingCommentIds = Lists.newArrayList();
        for (int i = 0; i < values.size(); i++) {
            if (Objects.isNull(values.get(i))) {
                missingCommentIds.add(parentIds.get(i));
            }
        }

        // 存在的一级评论 ID, 说明表中对应记录的 first_reply_comment_id 已经有值
        if (CollUtil.isNotEmpty(missingCommentIds)) {
            // 不存在的, 则需要进一步查询数据库来确定是否要更新记录对应的 first_reply_comment_id 值
            List<CommentDO> commentDOS = commentDOMapper.selectByCommentIds(missingCommentIds);

            // 异步将 first_reply_comment_id 不为 0 的一级评论 ID, 同步到 redis 中
            threadPoolTaskExecutor.submit(() -> {
                List<Long> needSyncCommentIds = commentDOS.stream()
                        .filter(commentDO -> commentDO.getFirstReplyCommentId() != 0)
                        .map(CommentDO::getId)
                        .toList();

                sync2Redis(needSyncCommentIds);
            });

            // 过滤出值为 0 的, 都需要更新 first_reply_comment_id
            List<CommentDO> needUpdateCommentDOS = commentDOS.stream()
                    .filter(commentDO -> commentDO.getFirstReplyCommentId() == 0)
                    .toList();
            needUpdateCommentDOS.forEach(commentDO -> {
                Long needUpdateCommentId = commentDO.getId();
                
                // 最早回复的那条评论
                CommentDO earliestCommentDO = commentDOMapper.selectEarliestByParentId(needUpdateCommentId);
                if (Objects.nonNull(earliestCommentDO)) {
                    // 最早回复的那条评论 ID
                    Long earliestCommentId = earliestCommentDO.getId();
                    
                    commentDOMapper.updateFirstReplyCommentIdByPrimaryKey(earliestCommentId, needUpdateCommentId);
                    
                    // 处理完更新后, 也需要继续异步同步到 redis
                    threadPoolTaskExecutor.submit(() -> sync2Redis(Lists.newArrayList(needUpdateCommentId)));
                }
            });
        }
    }

    /**
     * 同步到 redis（用管道一批次发送多条命令, 防止频繁操作 redis）
     *
     * @param needSyncCommentIds
     */
    private void sync2Redis(List<Long> needSyncCommentIds) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();

        // 使用 RedisTemplate 的管道模式, 允许在一个操作中批量发送多个命令，防止频繁操作 Redis
        redisTemplate.executePipelined((RedisCallback<?>) (connection) -> {
            needSyncCommentIds.forEach(needSyncCommentId -> {
                String key = RedisKeyConstants.buildHaveFirstReplyCommentKey(needSyncCommentId);

                // 批量设置值并指定过期时间（5小时以内）
                operations.set(key, 1, RandomUtil.randomInt(60 * 60 * 5), TimeUnit.SECONDS);
            });
            return null;
        });
    }

}
