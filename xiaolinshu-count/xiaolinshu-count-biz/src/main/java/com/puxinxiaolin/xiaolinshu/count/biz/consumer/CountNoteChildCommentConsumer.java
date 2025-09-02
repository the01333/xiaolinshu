package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.github.phantomthief.collection.BufferTrigger;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.count.biz.enums.CommentLevelEnum;
import com.puxinxiaolin.xiaolinshu.count.biz.model.dto.CountPublishCommentMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RocketMQMessageListener(consumerGroup = "xiaolinshu_group_child_comment_total" + MQConstants.TOPIC_COUNT_NOTE_COMMENT,
        topic = MQConstants.TOPIC_COUNT_NOTE_COMMENT
)
@Slf4j
public class CountNoteChildCommentConsumer implements RocketMQListener<String> {
    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    // 聚合处理
    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000) // 缓存队列的最大容量
            .batchSize(1000)   // 一批次最多聚合 1000 条
            .linger(Duration.ofSeconds(1)) // 多久聚合一次（1s 一次）
            .setConsumerEx(this::consumeMessage) // 设置消费者方法
            .build();

    @Override
    public void onMessage(String body) {
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 【笔记二级评论数】聚合消息, size: {}", bodys.size());
        log.info("==> 【笔记二级评论数】聚合消息, {}", JsonUtils.toJsonString(bodys));

        List<CountPublishCommentMqDTO> countPublishCommentMqDTOList = Lists.newArrayList();
        bodys.forEach(body -> {
            try {
                List<CountPublishCommentMqDTO> list = JsonUtils.parseList(body, CountPublishCommentMqDTO.class);
                countPublishCommentMqDTOList.addAll(list);
            } catch (Exception e) {
                log.error("", e);
            }
        });

        // 过滤出二级评论，并按 parent_id 分组
        Map<Long, List<CountPublishCommentMqDTO>> groupMap = countPublishCommentMqDTOList.stream()
                .filter(commentMqDTO -> Objects.equals(CommentLevelEnum.TWO.getCode(), commentMqDTO.getLevel()))
                .collect(Collectors.groupingBy(CountPublishCommentMqDTO::getParentId)); // 按 parent_id 分组

        // 若无二级评论，则直接 return
        if (CollUtil.isEmpty(groupMap)) return;

        // 循环分组字典
        for (Map.Entry<Long, List<CountPublishCommentMqDTO>> entry : groupMap.entrySet()) {
            // 一级评论 ID
            Long parentId = entry.getKey();
            // 评论数
            int count = CollUtil.size(entry.getValue());

            // 更新 redis 中的评论计数数据
            String key = RedisKeyConstants.buildCountCommentKey(parentId);
            Boolean hasKey = redisTemplate.hasKey(key);
            if (hasKey) {
                redisTemplate.opsForHash()
                        .increment(key, RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, count);
            }
            
            // 更新一级评论的下级评论总数，进行累加操作
            commentDOMapper.updateChildCommentTotal(parentId, count);
        }
        
        // ----------------------- 当一级评论被回复计数入库后, 重新计算一级评论的热度值 -----------------------------
        // 获取字典里的所有评论 ID
        Set<Long> commentIds = groupMap.keySet();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(commentIds))
                .build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COMMENT_HEAT_UPDATE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论热度值更新】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("==> 【评论热度值更新】MQ 发送异常:{}", e.getMessage(), e);
            }
        });
    }
}

