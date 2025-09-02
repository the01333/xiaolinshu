package com.puxinxiaolin.xiaolinshu.comment.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject.CommentDO;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.bo.CommentHeatBO;
import com.puxinxiaolin.xiaolinshu.comment.biz.util.HearCalculator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_child_" + MQConstants.TOPIC_COMMENT_HEAT_UPDATE,
        topic = MQConstants.TOPIC_COMMENT_HEAT_UPDATE
)
@Component
@Slf4j
public class CommentHeatUpdateConsumer implements RocketMQListener<String> {
    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000)
            .batchSize(300)
            .linger(Duration.ofSeconds(2))
            .setConsumerEx(this::consumeMessage)
            .build();

    @Override
    public void onMessage(String message) {
        bufferTrigger.enqueue(message);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 【评论热度值计算】聚合消息, size: {}", bodys.size());
        log.info("==> 【评论热度值计算】聚合消息, {}", JsonUtils.toJsonString(bodys));

        Set<Long> commentIds = Sets.newHashSet();
        bodys.forEach(body -> {
            try {
                Set<Long> eachCommentIds = JsonUtils.parseSet(body, Long.class);

                commentIds.addAll(eachCommentIds);
            } catch (Exception e) {
                log.error("", e);
            }
        });

        log.info("==> 去重后的评论 ID: {}", commentIds);

        List<CommentDO> commentDOS = commentDOMapper.selectByCommentIds(commentIds.stream().toList());

        List<Long> resultCommentIds = Lists.newArrayList();
        List<CommentHeatBO> commentHeatBOS = Lists.newArrayList();
        commentDOS.forEach(commentHeatDO -> {
            Long commentId = commentHeatDO.getId();
            Long likeTotal = commentHeatDO.getLikeTotal();
            Long childCommentTotal = commentHeatDO.getChildCommentTotal();

            BigDecimal heatNum = HearCalculator.calculateHeat(likeTotal, childCommentTotal);
            resultCommentIds.add(commentId);
            commentHeatBOS.add(CommentHeatBO.builder()
                    .id(commentId)
                    .heat(heatNum.doubleValue())
                    .noteId(commentHeatDO.getNoteId())
                    .build());
        });

        int count = commentDOMapper.batchUpdateHeatByCommentIds(resultCommentIds, commentHeatBOS);
        if (count == 0) return;

        // 更新 redis 中 ZSET 的热度值
        updateRedisComments(commentHeatBOS);
    }

    /**
     * 更新 redis 中 ZSET 的热度值
     *
     * @param commentHeatBOS
     */
    private void updateRedisComments(List<CommentHeatBO> commentHeatBOS) {
        // 过滤出热度值大于 0 的, 并按所属笔记 ID 分组（若热度等于0，则不进行更新）
        Map<Long, List<CommentHeatBO>> noteIdAndBOListMap = commentHeatBOS.stream()
                .filter(bo -> bo.getHeat() > 0)
                .collect(Collectors.groupingBy(CommentHeatBO::getNoteId));
        
        noteIdAndBOListMap.forEach((noteId, boList) -> {
            String key = RedisKeyConstants.buildCommentListKey(noteId);

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setResultType(Long.class);
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/update_hot_comments.lua")));
        
            List<Object> args = Lists.newArrayList();
            commentHeatBOS.forEach(bo -> {
                args.add(bo.getId());
                args.add(bo.getHeat());
            });

            redisTemplate.execute(script, Collections.singletonList(key), args.toArray());
        });
    }

}
