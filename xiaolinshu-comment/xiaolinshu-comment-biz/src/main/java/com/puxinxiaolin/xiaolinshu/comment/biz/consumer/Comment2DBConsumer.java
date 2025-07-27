package com.puxinxiaolin.xiaolinshu.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject.CommentDO;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.enums.CommentLevelEnum;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.bo.CommentBO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.PublishCommentMqDTO;
import com.puxinxiaolin.xiaolinshu.comment.biz.rpc.KeyValueRpcService;
import com.puxinxiaolin.xiaolinshu.kv.api.api.KeyValueFeignApi;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class Comment2DBConsumer {
    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private KeyValueRpcService keyValueRpcService;

    @Value("${rocketmq.name-server}")
    private String namesrvAddr;
    // TODO [YCcLin 2025/7/26]: 后续改为 nacos 动态配置 
    private RateLimiter rateLimiter = RateLimiter.create(1000);

    private DefaultMQPushConsumer consumer;

    @Bean
    public DefaultMQPushConsumer defaultMQPushConsumer() throws MQClientException {
        String consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_PUBLISH_COMMENT;

        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.subscribe(MQConstants.TOPIC_PUBLISH_COMMENT, "*");
        // 设置消费者消费消息的起始位置, 如果队列中没有消息, 则从最新的消息开始消费。
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setMessageModel(MessageModel.CLUSTERING);
        consumer.setConsumeMessageBatchMaxSize(30);

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            log.info("==> 本批次消息大小: {}", msgs.size());

            try {
                rateLimiter.acquire();

                List<PublishCommentMqDTO> publishCommentMqDTOS = Lists.newArrayList();
                msgs.forEach(msg -> {
                    String message = new String(msg.getBody());
                    log.info("==> Consumer - Received message: {}", message);

                    publishCommentMqDTOS.add(JsonUtils.parseObject(message, PublishCommentMqDTO.class));
                });

                // 提取所有不为空的回复评论 ID
                List<Long> replyCommentIds = publishCommentMqDTOS.stream()
                        .map(PublishCommentMqDTO::getReplyCommentId)
                        .filter(Objects::nonNull)
                        .toList();
                // 批量查询相关回复评论记录
                List<CommentDO> replyCommentDOS = null;
                if (CollUtil.isNotEmpty(replyCommentIds)) {
                    replyCommentDOS = commentDOMapper.selectByCommentIds(replyCommentIds);
                }

                // 构建回复的评论 DO - Map<commentId, commentDO>
                Map<Long, CommentDO> commentIdAndCommentDOMap = Maps.newHashMap();
                if (CollUtil.isNotEmpty(replyCommentDOS)) {
                    commentIdAndCommentDOMap = replyCommentDOS.stream()
                            .collect(Collectors.toMap(CommentDO::getId, commentDO -> commentDO));
                }

                List<CommentBO> commentBOS = Lists.newArrayList();
                for (PublishCommentMqDTO mqDTO : publishCommentMqDTOS) {
                    String imageUrl = mqDTO.getImageUrl();
                    CommentBO commentBO = CommentBO.builder()
                            .id(mqDTO.getCommentId())
                            .noteId(mqDTO.getNoteId())
                            .userId(mqDTO.getCreatorId())
                            .isContentEmpty(true)   // 默认评论内容为空
                            .imageUrl(StringUtils.isBlank(imageUrl) ? "" : imageUrl)
                            .level(CommentLevelEnum.ONE.getCode())   // 默认为一级评论
                            .parentId(mqDTO.getNoteId())   // 默认为所属笔记 ID
                            .createTime(mqDTO.getCreateTime())
                            .updateTime(mqDTO.getCreateTime())
                            .isTop(false)
                            .replyTotal(0L)
                            .likeTotal(0L)
                            .replyUserId(0L)
                            .replyCommentId(0L)
                            .build();

                    String content = mqDTO.getContent();
                    if (StringUtils.isNotBlank(content)) {
                        commentBO.setContentUuid(UUID.randomUUID().toString());
                        commentBO.setIsContentEmpty(false);
                        commentBO.setContent(content);
                    }
                    
                    Long replyCommentId = mqDTO.getReplyCommentId();
                    if (Objects.nonNull(replyCommentId)) {
                        CommentDO replyCommentDO = commentIdAndCommentDOMap.get(replyCommentId);
                        if (Objects.nonNull(replyCommentDO)) {
                            // 若回复的评论 ID 不为空, 说明是二级评论
                            commentBO.setLevel(CommentLevelEnum.TWO.getCode());
                            commentBO.setReplyCommentId(replyCommentId);
                            commentBO.setParentId(replyCommentDO.getId());
                            if (Objects.equals(replyCommentDO.getLevel(), CommentLevelEnum.TWO.getCode())) {
                                 commentBO.setParentId(replyCommentDO.getParentId());
                            }
                            commentBO.setReplyUserId(replyCommentDO.getUserId());
                        }
                    }
                    
                    commentBOS.add(commentBO);
                }

                log.info("## 清洗后的 CommentBOS: {}", JsonUtils.toJsonString(commentBOS));

                // TODO [YCcLin 2025/7/27]: 后续处理
                transactionTemplate.execute(status -> {
                    try {
                        commentDOMapper.batchInsert(commentBOS);

                        // 过滤评论内容不为空的 BO
                        List<CommentBO> existedCommentBO = commentBOS.stream()
                                .filter(bo -> Boolean.FALSE.equals(bo.getIsContentEmpty()))
                                .toList();
                        if (CollUtil.isNotEmpty(existedCommentBO)) {
                            keyValueRpcService.batchSaveCommentContent(existedCommentBO);
                        }

                        return true;
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        log.error("", e);
                        
                        throw e;
                    }
                });

                // 手动 ACK, 告诉 RocketMQ 这批次消息消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } catch (Exception e) {
                log.error("", e);

                // 手动 ACK, 告诉 RocketMQ 这批次消息处理失败, 稍后再进行重试
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        });

        consumer.start();
        return consumer;
    }

    @PreDestroy
    public void destroy() {
        if (Objects.nonNull(consumer)) {
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

}
