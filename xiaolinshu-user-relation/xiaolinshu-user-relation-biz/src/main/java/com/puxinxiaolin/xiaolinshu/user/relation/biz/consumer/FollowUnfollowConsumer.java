package com.puxinxiaolin.xiaolinshu.user.relation.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.puxinxiaolin.framework.common.util.DateUtils;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.constant.MqConstants;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.dataobject.FansDO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.dataobject.FollowingDO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper.FansDOMapper;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.dto.FollowUserMqDTO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.dto.UnfollowUserMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MqConstants.TOPIC_FOLLOW_OR_UNFOLLOW,
        topic = MqConstants.TOPIC_FOLLOW_OR_UNFOLLOW,
        consumeMode = ConsumeMode.ORDERLY
)
public class FollowUnfollowConsumer implements RocketMQListener<Message> {
    @Resource
    private FollowingDOMapper followingDOMapper;
    @Resource
    private FansDOMapper fansDOMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    // 用令牌桶实现 MQ 流量削峰
    @Resource
    private RateLimiter rateLimiter;

    @Override
    public void onMessage(Message message) {
        // 流量削峰: 获取到令牌才往下执行, 否则阻塞
        rateLimiter.acquire();

        String msgJson = new String(message.getBody());
        String tags = message.getTags();

        log.info("==> FollowUnfollowConsumer 消费了消息: {}, tags: {}", msgJson, tags);

        if (Objects.equals(tags, MqConstants.TAG_FOLLOW)) {
            handleFollowTagMessage(msgJson);
        } else if (Objects.equals(tags, MqConstants.TAG_UNFOLLOW)) {
            // 取关
            handleUnfollowTagMessage(msgJson);
        }
    }

    /**
     * 取关
     *
     * @param msgJson
     */
    private void handleUnfollowTagMessage(String msgJson) {
        UnfollowUserMqDTO mqDTO = JsonUtils.parseObject(msgJson, UnfollowUserMqDTO.class);
        if (mqDTO == null) {
            return;
        }

        Long userId = mqDTO.getUserId();
        Long unfollowUserId = mqDTO.getUnfollowUserId();

        Boolean isSuccess = transactionTemplate.execute(status -> {
            try {
                int count = followingDOMapper.deleteByUserIdAndFollowingUserId(userId, unfollowUserId);
                if (count > 0) {
                    fansDOMapper.deleteByUserIdAndFansUserId(unfollowUserId, userId);
                }

                return true;
            } catch (Exception ex) {
                status.setRollbackOnly();
                log.error("{}", ex.getMessage(), ex);
            }

            return false;
        });
        
        // 将自己从被取关用户的 ZSet 粉丝列表删除
        if (Boolean.TRUE.equals(isSuccess)) {
            String key = RedisKeyConstants.buildUserFansKey(unfollowUserId);
            redisTemplate.opsForZSet().remove(key, userId);
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
                log.error("{}", e.getMessage(), e);
            }

            return false;
        });

        log.info("## 数据库添加记录结果: {}", isSuccess);
        //  更新 Redis 中被关注用户的 ZSet 粉丝列表
        if (Boolean.TRUE.equals(isSuccess)) {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_update_fans_zset.lua")));
            script.setResultType(Long.class);

            long timestamp = DateUtils.localDateTime2Timestamp(createTime);

            String key = RedisKeyConstants.buildUserFansKey(followUserId);
            redisTemplate.execute(script, Collections.singletonList(key), userId, timestamp);
        }
    }

}
