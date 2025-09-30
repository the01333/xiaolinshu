package com.puxinxiaolin.xiaolinshu.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject.CommentDO;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.enums.CommentLevelEnum;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.bo.CommentBO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.CountPublishCommentMqDTO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.PublishCommentMqDTO;
import com.puxinxiaolin.xiaolinshu.comment.biz.rpc.KeyValueRpcService;
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
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scripting.support.ResourceScriptSource;
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
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${rocketmq.name-server}")
    private String namesrvAddr;
    // TODO [YCcLin 2025/7/26]: 后续改为 nacos 动态配置 
    private RateLimiter rateLimiter = RateLimiter.create(1000);

    private DefaultMQPushConsumer consumer;

    /**
     * 手动消费 MQ 消息
     *
     * @return
     * @throws MQClientException
     */
    @Bean
    public DefaultMQPushConsumer comment2DBMQPushConsumer() throws MQClientException {
        String consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_PUBLISH_COMMENT;

        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.subscribe(MQConstants.TOPIC_PUBLISH_COMMENT, "*");
        // 设置消费者消费消息的起始位置, 如果队列中没有消息, 则从最新的消息开始消费
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

                Integer insertedRows = transactionTemplate.execute(status -> {
                    try {
                        int count = commentDOMapper.batchInsert(commentBOS);

                        // 过滤评论内容不为空的 BO
                        List<CommentBO> existedCommentBO = commentBOS.stream()
                                .filter(bo -> Boolean.FALSE.equals(bo.getIsContentEmpty()))
                                .toList();
                        if (CollUtil.isNotEmpty(existedCommentBO)) {
                            keyValueRpcService.batchSaveCommentContent(existedCommentBO);
                        }

                        return count;
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        log.error("", e);

                        throw e;
                    }
                });

                if (Objects.nonNull(insertedRows) && insertedRows > 0) {
                    List<CountPublishCommentMqDTO> countPublishCommentMqDTOS = commentBOS.stream()
                            .map(commentBO -> CountPublishCommentMqDTO.builder()
                                    .noteId(commentBO.getNoteId())
                                    .commentId(commentBO.getId())
                                    .level(commentBO.getLevel())
                                    .parentId(commentBO.getParentId())
                                    .build()
                            ).toList();

                    Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countPublishCommentMqDTOS))
                            .build();

                    // 同步一级评论到 redis 热点评论 ZSET 中
                    syncOneLevelComment2RedisZSet(commentBOS);

                    rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COMMENT, message, new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("==> 【计数: 评论发布】MQ 发送成功, SendResult: {}", sendResult);
                        }

                        @Override
                        public void onException(Throwable e) {
                            log.error("==> 【计数: 评论发布】MQ 发送异常: ", e);
                        }
                    });
                }

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

    /**
     * 同步一级评论到 redis 热点评论 ZSET 中
     *
     * @param commentBOS
     */
    private void syncOneLevelComment2RedisZSet(List<CommentBO> commentBOS) {
        // 过滤出一级评论, 并按 noteId 进行分组
        Map<Long, List<CommentBO>> noteIdAndBOListMap = commentBOS.stream()
                .filter(commentBO -> Objects.equals(commentBO.getLevel(), CommentLevelEnum.ONE.getCode()))
                .collect(Collectors.groupingBy(CommentBO::getNoteId));
        
        noteIdAndBOListMap.forEach((noteId, commentBOList) -> {
            String key = RedisKeyConstants.buildCommentListKey(noteId);

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setResultType(Long.class);
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/add_hot_comments.lua")));

            List<Object> args = Lists.newArrayList();
            commentBOList.forEach(commentBO -> {
                args.add(commentBO.getId());
                // 新增的评论热度值肯定为 0
                args.add(0);
            });
            
            // 走 lua 脚本
            redisTemplate.execute(script, Collections.singletonList(key), args.toArray());
        });
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
