package com.puxinxiaolin.xiaolinshu.comment.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject.CommentDO;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.bo.CommentHeatBO;
import com.puxinxiaolin.xiaolinshu.comment.biz.util.HearCalculator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_child_" + MQConstants.TOPIC_COMMENT_HEAT_UPDATE,
        topic = MQConstants.TOPIC_COMMENT_HEAT_UPDATE
)
@Component
@Slf4j
public class CommentHeatUpdateConsumer implements RocketMQListener<String> {
    @Resource
    private CommentDOMapper commentDOMapper;

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
        List<CommentHeatBO> commentBOS = Lists.newArrayList();
        commentDOS.forEach(commentDO -> {
            Long commentId = commentDO.getId();
            Long likeTotal = commentDO.getLikeTotal();
            Long childCommentTotal = commentDO.getChildCommentTotal();

            BigDecimal heatNum = HearCalculator.calculateHeat(likeTotal, childCommentTotal);
            resultCommentIds.add(commentId);
            commentBOS.add(CommentHeatBO.builder()
                    .id(commentId)
                    .heat(heatNum.doubleValue())
                    .build());
        });

        commentDOMapper.batchUpdateHeatByCommentIds(resultCommentIds, commentBOS);
    }

}
