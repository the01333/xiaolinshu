package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.enums.LikeUnlikeCommentTypeEnum;
import com.puxinxiaolin.xiaolinshu.count.biz.model.dto.AggregationCountLikeUnlikeCommentMqDTO;
import com.puxinxiaolin.xiaolinshu.count.biz.model.dto.CountLikeUnlikeCommentMqDTO;
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
import java.util.stream.Collectors;

@RocketMQMessageListener(
        topic = MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE,
        consumerGroup = "xiaolinshu_group_count_" + MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE
)
@Component
@Slf4j
public class CountCommentLikeConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000)
            .batchSize(1000)  // 一批次最多聚合 1000 条
            .linger(Duration.ofSeconds(1))
            .setConsumerEx(this::consumeMessage)
            .build();

    @Override
    public void onMessage(String message) {
        bufferTrigger.enqueue(message);
    }
    
    private void consumeMessage(List<String> messages) {
        log.info("==> 【评论点赞数】聚合消息, size: {}", messages.size());
        log.info("==> 【评论点赞数】聚合消息, {}", JsonUtils.toJsonString(messages));

        List<CountLikeUnlikeCommentMqDTO> countLikeUnlikeCommentMqDTOS = messages.stream()
                .map(body -> JsonUtils.parseObject(body, CountLikeUnlikeCommentMqDTO.class))
                .toList();
        // 按评论 ID 进行分组
        Map<Long, List<CountLikeUnlikeCommentMqDTO>> groupMap = countLikeUnlikeCommentMqDTOS.stream()
                .collect(Collectors.groupingBy(CountLikeUnlikeCommentMqDTO::getCommentId));

        // 按组汇总数据, 统计出最终的计数
        // 最终操作的计数对象
        List<AggregationCountLikeUnlikeCommentMqDTO> countList = Lists.newArrayList();
        for (Map.Entry<Long, List<CountLikeUnlikeCommentMqDTO>> entry : groupMap.entrySet()) {
            Long commentId = entry.getKey();
            List<CountLikeUnlikeCommentMqDTO> mqDTOList = entry.getValue();
            int finalCount = 0;
            for (CountLikeUnlikeCommentMqDTO mqDTO : mqDTOList) {
                Integer type = mqDTO.getType();
                LikeUnlikeCommentTypeEnum likeUnlikeCommentTypeEnum = LikeUnlikeCommentTypeEnum.valueOf(type);
                if (Objects.isNull(likeUnlikeCommentTypeEnum)) continue;
                
                switch (likeUnlikeCommentTypeEnum) {
                    case LIKE -> finalCount += 1;
                    case UNLIKE -> finalCount -= 1;
                }
            }
            
            // 把最终的计数和评论 ID 加入 countList
            countList.add(AggregationCountLikeUnlikeCommentMqDTO.builder()
                    .commentId(commentId)
                    .count(finalCount)
                    .build()
            );
        }

        log.info("## 【评论点赞数】聚合后的计数数据: {}", JsonUtils.toJsonString(countList));

        // 更新缓存计数
        countList.forEach(item -> {
            Long commentId = item.getCommentId();
            Integer count = item.getCount();

            String redisKey = RedisKeyConstants.buildCountCommentKey(commentId);
            Boolean hasKey = redisTemplate.hasKey(redisKey);
            // 因为缓存设有过期时间, 这里需要判断一下, 存在才会去更新, 而初始化工作放在查询计数来做
            if (hasKey) {
                redisTemplate.opsForHash()
                        .increment(redisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
            }
        });
        
        // 走 MQ, 确保数据入库
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countList))
                .build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_COMMENT_LIKE_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：评论点赞数写库】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：评论点赞数写库】MQ 发送异常: ", throwable);
            }
        });
    }

}
