package com.puxinxiaolin.xiaolinshu.user.relation.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.constant.MqConstants;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.dataobject.FansDO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.dataobject.FollowingDO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper.FansDOMapper;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.dto.FollowUserMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group",
        topic = MqConstants.TOPIC_FOLLOW_OR_UNFOLLOW
)
public class FollowUnfollowConsumer implements RocketMQListener<Message> {
    @Resource
    private FollowingDOMapper followingDOMapper;
    @Resource
    private FansDOMapper fansDOMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    // 用令牌桶实现 MQ 流量削峰
    @Resource
    private RateLimiter rateLimiter;

    @Override
    public void onMessage(Message message) {
        // 获取到令牌才往下执行, 否则阻塞
        rateLimiter.acquire();
        
        String msgJson = new String(message.getBody());
        String tags = message.getTags();

        log.info("==> FollowUnfollowConsumer 消费了消息: {}, tags: {}", msgJson, tags);

        if (Objects.equals(tags, MqConstants.TAG_FOLLOW)) {
            handleFollowTagMessage(msgJson);
        } else if (Objects.equals(tags, MqConstants.TAG_UNFOLLOW)) {
            // TODO [YCcLin 2025/6/5]: 取关
        }
    }

    /**
     * 关注
     *
     * @param msgJson
     */
    private void handleFollowTagMessage(String msgJson) {
        FollowUserMqDTO mqDTO = JsonUtils.parseObject(msgJson, FollowUserMqDTO.class);
        if (mqDTO == null) {
            return;
        }

        // 幂等性: 通过唯一索引保证
        Long userId = mqDTO.getUserId();
        Long followUserId = mqDTO.getFollowUserId();
        LocalDateTime createTime = mqDTO.getCreateTime();

        Boolean isSuccess = transactionTemplate.execute(status -> {
            try {
                int count = followingDOMapper.insert(FollowingDO.builder()
                        .userId(userId)
                        .followingUserId(followUserId)
                        .createTime(createTime)
                        .build());
                if (count > 0) {
                    fansDOMapper.insert(FansDO.builder()
                            .fansUserId(userId)
                            .userId(followUserId)
                            .createTime(createTime)
                            .build());
                }

                return true;
            } catch (Exception e) {
                status.setRollbackOnly();   // 标记事务为回滚
                log.error("", e.getMessage(), e);
            }

            return false;
        });

        log.info("## 数据库添加记录结果: {}", isSuccess);
        // TODO [YCcLin 2025/6/5]: 更新 Redis 中被关注用户的 ZSet 粉丝列表 
    }

}
