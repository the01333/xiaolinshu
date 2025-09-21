package com.puxinxiaolin.xiaolinshu.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentLikeDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.enums.LikeUnlikeCommentTypeEnum;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.LikeUnlikeCommentMqDTO;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LikeUnlikeComment2DBConsumer {

    @Resource
    private CommentLikeDOMapper commentLikeDOMapper;
    
    @Value("${rocketmq.name-server}")
    private String namesrvAddr;

    private DefaultMQPushConsumer consumer;

    // 每秒创建 5000 个令牌
    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Bean
    public DefaultMQPushConsumer defaultMQPushConsumer() throws MQClientException {
        String group = "xiaolinshu_group_" + MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE;

        consumer = new DefaultMQPushConsumer(group);
        consumer.setNamesrvAddr(namesrvAddr);
        // 订阅指定的主题, 并设置主题的订阅规则（"*" 表示订阅所有标签的消息）
        consumer.subscribe(MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE, "*");
        // 设置消费者消费消息的起始位置, 如果队列中没有消息, 则从最新的消息开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setMessageModel(MessageModel.CLUSTERING);
        // 最大重试次数
        consumer.setMaxReconsumeTimes(3);
        // 设置每批次消费的最大消息数量, 表示每次拉取时最多消费 30 条消息
        consumer.setConsumeMessageBatchMaxSize(30);
        // 注册消息监听器
        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            log.info("==> 【评论点赞、取消点赞】本批次消息大小: {}", msgs.size());
            try {
                rateLimiter.acquire();

                List<LikeUnlikeCommentMqDTO> mqDTOList = Lists.newArrayList();

                msgs.forEach(msg -> {
                    String tag = msg.getTags();
                    String messageJson = new String(msg.getBody());
                    log.info("==> 【评论点赞、取消点赞】Consumer - Tag: {}, Received message: {}", tag, messageJson);

                    mqDTOList.add(JsonUtils.parseObject(messageJson, LikeUnlikeCommentMqDTO.class));
                });

                // 按评论 ID 分组
                Map<Long, List<LikeUnlikeCommentMqDTO>> commentIdAndListMap = mqDTOList.stream()
                        .collect(Collectors.groupingBy(LikeUnlikeCommentMqDTO::getCommentId));
                List<LikeUnlikeCommentMqDTO> finalLikeUnlikeCommentMqDTOS = Lists.newArrayList();
                commentIdAndListMap.forEach((commentId, mqDTOS) -> {
                    // 优化: 若某个用户对某评论多次操作, 如点赞 -> 取消点赞 -> 点赞, 需进行操作合并, 只提取最后一次操作, 进一步降低操作数据库的频率
                    Map<Long, LikeUnlikeCommentMqDTO> userLastOperateMqDTOS = mqDTOS.stream()
                            .collect(Collectors.toMap(
                                    // 用户 ID 为 key
                                    LikeUnlikeCommentMqDTO::getUserId,
                                    // 把自己的入参作为返参, 这里是 LikeUnlikeCommentMqDTO
                                    Function.identity(),
                                    // 合并策略: 当出现重复键（同一用户多次操作）时, 保留时间更晚的记录
                                    (oldValue, newValue) ->
                                            oldValue.getCreateTime().isAfter(newValue.getCreateTime()) ? oldValue : newValue
                            ));

                    finalLikeUnlikeCommentMqDTOS.addAll(userLastOperateMqDTOS.values());
                });

                // 批量操作数据库
                executeBatchSQL(finalLikeUnlikeCommentMqDTOS);

                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Exception e) {
                log.error("", e);
                // 这样 RocketMQ 会暂停当前队列的消费一段时间再重试
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        });

        consumer.start();
        return consumer;
    }

    /**
     * 批量操作数据库
     *
     * @param mqDTOList
     */
    private void executeBatchSQL(List<LikeUnlikeCommentMqDTO> mqDTOList) {
        // 过滤出点赞记录
        List<LikeUnlikeCommentMqDTO> likes = mqDTOList.stream()
                .filter(mqDTO -> Objects.equals(mqDTO.getType(), LikeUnlikeCommentTypeEnum.LIKE.getCode()))
                .toList();
        // 过滤出取消点赞记录
        List<LikeUnlikeCommentMqDTO> unlikes = mqDTOList.stream()
                .filter(mqDTO -> Objects.equals(mqDTO.getType(), LikeUnlikeCommentTypeEnum.UNLIKE.getCode()))
                .toList();
        
        // 取消点赞
        if (CollUtil.isNotEmpty(unlikes)) {
            commentLikeDOMapper.batchDelete(unlikes);
        }
        // 点赞
        if (CollUtil.isNotEmpty(likes)) {
            commentLikeDOMapper.batchInsert(likes);
        }
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

}
