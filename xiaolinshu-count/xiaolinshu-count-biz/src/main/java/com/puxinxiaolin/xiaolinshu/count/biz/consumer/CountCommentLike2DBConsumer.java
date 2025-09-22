package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.count.biz.model.dto.AggregationCountLikeUnlikeCommentMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RocketMQMessageListener(consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_COUNT_COMMENT_LIKE_2_DB, // Group 组
        topic = MQConstants.TOPIC_COUNT_COMMENT_LIKE_2_DB
)
@Slf4j
public class CountCommentLike2DBConsumer implements RocketMQListener<String> {

    @Resource
    private CommentDOMapper commentDOMapper;

    // 每秒创建 5000 个令牌
    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(String message) {
        rateLimiter.acquire();

        log.info("## 消费到了 MQ 【计数: 评论点赞数入库】, {}...", message);

        List<AggregationCountLikeUnlikeCommentMqDTO> countList = null;
        try {
            countList = JsonUtils.parseList(message, AggregationCountLikeUnlikeCommentMqDTO.class);
        } catch (Exception e) {
            log.error("## 解析 JSON 字符串异常", e);
        }

        if (CollUtil.isNotEmpty(countList)) {
            countList.forEach(item -> {
                Integer count = item.getCount();
                Long commentId = item.getCommentId();

                commentDOMapper.updateLikeTotalByCommentId(count, commentId);
            });
        }
    }

}
