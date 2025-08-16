package com.puxinxiaolin.xiaolinshu.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject.CommentDO;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.enums.CommentLevelEnum;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.bo.CommentBO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.bo.CommentHeatBO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.CountPublishCommentMqDTO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.PublishCommentMqDTO;
import com.puxinxiaolin.xiaolinshu.comment.biz.rpc.KeyValueRpcService;
import com.puxinxiaolin.xiaolinshu.comment.biz.util.HearCalculator;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

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
