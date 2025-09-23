package com.puxinxiaolin.xiaolinshu.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject.CommentDO;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.NoteCountDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.enums.CommentLevelEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_DELETE_COMMENT,
        topic = MQConstants.TOPIC_DELETE_COMMENT
)
public class DeleteCommentConsumer implements RocketMQListener<String> {
    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private NoteCountDOMapper noteCountDOMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private RateLimiter rateLimiter = RateLimiter.create(1000);

    @Override
    public void onMessage(String message) {
        rateLimiter.acquire();

        log.info("## 【删除评论 - 后续业务处理】消费者消费成功, body: {}", message);

        CommentDO commentDO = JsonUtils.parseObject(message, CommentDO.class);
        Integer level = commentDO.getLevel();
        CommentLevelEnum commentLevelEnum = CommentLevelEnum.valueOf(level);
        switch (commentLevelEnum) {
            case ONE -> handleOneLevelComment(commentDO);
            case TWO -> handleTwoLevelComment(commentDO);
        }
    }

    /**
     * 处理二级评论
     *
     * @param commentDO
     */
    private void handleTwoLevelComment(CommentDO commentDO) {
        Long commentId = commentDO.getId();

        // 1. 删除关联评论
        List<Long> replyCommentIds = Lists.newArrayList();
        // 递归拿到所有回复评论
        recurrentGetReplyCommentId(replyCommentIds, commentId);

        int count = 0;
        if (CollUtil.isNotEmpty(replyCommentIds)) {
            count = commentDOMapper.deleteByIds(replyCommentIds);
        }

        // 2. 更新一级评论的计数
        Long parentCommentId = commentDO.getParentId();
        String redisKey = RedisKeyConstants.buildCountCommentKey(parentCommentId);
        Boolean hasKey = redisTemplate.hasKey(redisKey);
        if (hasKey) {
            // 还包括要删除二级评论自身, 所以要 + 1
            redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_COMMENT_TOTAL, -(count + 1));
        }

        // 3. 若是最早的发布的二级评论被删除, 需要更新一级评论的 first_reply_comment_id
        CommentDO oneLevelCommentDO = commentDOMapper.selectByPrimaryKey(parentCommentId);
        Long firstReplyCommentId = oneLevelCommentDO.getFirstReplyCommentId();
        // 若删除的是最早回复的二级评论
        if (Objects.equals(commentId, firstReplyCommentId)) {
            // 重新获取一级评论的最早回复
            CommentDO earliestByParentId = commentDOMapper.selectEarliestByParentId(parentCommentId);
            Long earliestCommentId = Objects.nonNull(earliestByParentId) ? earliestByParentId.getId() : null;
            
            // 更新其一级评论的 first_reply_comment_id
            commentDOMapper.updateFirstReplyCommentIdByPrimaryKey(earliestCommentId, parentCommentId);
        }

        // 4. 重新计算一级评论的热度值
        Set<Long> commentIds = Sets.newHashSetWithExpectedSize(1);
        commentIds.add(parentCommentId);

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(commentIds))
                .build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COMMENT_HEAT_UPDATE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论热度值更新】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("==> 【评论热度值更新】MQ 发送异常: ", e);
            }
        });
    }

    /**
     * 递归查询回复评论
     *
     * @param replyCommentIds
     * @param commentId
     */
    private void recurrentGetReplyCommentId(List<Long> replyCommentIds, Long commentId) {
        CommentDO replyCommentDO = commentDOMapper.selectByReplyCommentId(commentId);
        if (Objects.isNull(replyCommentDO)) return;
        
        replyCommentIds.add(replyCommentDO.getId());
        Long replyCommentId = replyCommentDO.getId();
        
        // 递归查找
        recurrentGetReplyCommentId(replyCommentIds, replyCommentId);
    }

    /**
     * 处理一级评论
     *
     * @param commentDO
     */
    private void handleOneLevelComment(CommentDO commentDO) {
        Long commentId = commentDO.getId();
        Long noteId = commentDO.getNoteId();

        // 1. 删除关联评论（一级评论下的所有子评论都需要删除）
        int count = commentDOMapper.deleteByParentId(commentId);

        // 2. 计数更新（笔记下的总评论数, 这里需要额外 + 1, 算上一级评论自身）
        String redisKey = RedisKeyConstants.buildNoteCommentTotalKey(noteId);
        Boolean hasKey = redisTemplate.hasKey(redisKey);
        if (hasKey) {
            redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_COMMENT_TOTAL, -(count + 1));
        }

        noteCountDOMapper.updateCommentTotalByNoteId(noteId, -(count + 1));
    }

}
