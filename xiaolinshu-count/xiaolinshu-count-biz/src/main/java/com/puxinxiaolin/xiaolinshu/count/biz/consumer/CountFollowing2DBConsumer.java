package com.puxinxiaolin.xiaolinshu.count.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.count.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.count.biz.domain.mapper.UserCountDOMapper;
import com.puxinxiaolin.xiaolinshu.count.biz.enums.FollowUnfollowTypeEnum;
import com.puxinxiaolin.xiaolinshu.count.biz.model.dto.CountFollowUnfollowMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_COUNT_FOLLOWING_2_DB,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING_2_DB
)
public class CountFollowing2DBConsumer implements RocketMQListener<String> {

    @Resource
    private UserCountDOMapper userCountDOMapper;
    @Resource
    private RateLimiter rateLimiter;

    @Override
    public void onMessage(String body) {
        // 流量削峰: 获取到令牌才往下执行, 否则阻塞
        rateLimiter.acquire();

        log.info("## 消费到了 MQ 【计数: 关注数入库】, {}...", body);
        if (StringUtils.isBlank(body)) {
            return;
        }
        
        CountFollowUnfollowMqDTO mqDTO = JsonUtils.parseObject(body, CountFollowUnfollowMqDTO.class);
        Integer type = mqDTO.getType();
        Long userId = mqDTO.getUserId();

        // 关注 +1, 取关 -1
        int count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;
        userCountDOMapper.insertOrUpdateFollowingTotalByUserId(count, userId);
    }

}
