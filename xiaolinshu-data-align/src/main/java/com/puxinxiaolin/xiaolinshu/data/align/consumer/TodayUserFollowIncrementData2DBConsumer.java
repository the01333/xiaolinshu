package com.puxinxiaolin.xiaolinshu.data.align.consumer;

import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.data.align.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.data.align.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.data.align.constant.TableConstants;
import com.puxinxiaolin.xiaolinshu.data.align.domain.mapper.InsertMapper;
import com.puxinxiaolin.xiaolinshu.data.align.model.dto.FollowUnfollowMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

/**
 * @Description: 这里我以关注的视角来写（也可以以被关注的视角写）
 * @Author: YCcLin
 * @Date: 2025/7/12 0:44
 */
@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_data_align_" + MQConstants.TOPIC_COUNT_FOLLOWING,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING
)
public class TodayUserFollowIncrementData2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private InsertMapper insertMapper;
    
    @Value("${table.shards}")
    private int tableShards;

    @Override
    public void onMessage(String message) {
        log.info("## TodayUserFollowIncrementData2DBConsumer 消费到了 MQ: {}", message);

        FollowUnfollowMqDTO mqDTO = JsonUtils.parseObject(message, FollowUnfollowMqDTO.class);
        if (Objects.isNull(mqDTO)) {
            return;
        }

        Long userId = mqDTO.getUserId();
        Long targetUserId = mqDTO.getTargetUserId();

        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // ------------------------- 源用户的关注数变更记录 -------------------------
        String userRedisKey = RedisKeyConstants.buildBloomUserFollowListKey(date);

        // 布隆过滤器判断该日增量数据是否已经记录
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_user_follow_check.lua")));

        Long result = redisTemplate.execute(script, Collections.singletonList(userRedisKey), userId);
        if (Objects.equals(result, 0L)) {
            // 若无，才会落库，减轻数据库压力
            long userIdHashKey = userId % tableShards;
            
            try {
                // 将日增量变更数据，写入表 t_data_align_following_count_temp_日期_分片序号
                insertMapper.insert2DataAlignUserFollowingCountTempTable(TableConstants.buildTableNameSuffix(date, userIdHashKey), userId);
            } catch (Exception e) {
                log.error("{}", e.getMessage(), e);
            }

            // 数据库写入成功后，再添加布隆过滤器中
            redisTemplate.execute(script, Collections.singletonList(userRedisKey), userId);
        }

        // ------------------------- 目标用户的粉丝数变更记录 -------------------------
        String targetUserRedisKey = RedisKeyConstants.buildBloomUserFansListKey(date);

        // 布隆过滤器判断该日增量数据是否已经记录
        result = redisTemplate.execute(script, Collections.singletonList(targetUserRedisKey), targetUserId);
        if (Objects.equals(result, 0L)) {
            // 若无，才会落库，减轻数据库压力
            long targetUserIdHashKey = targetUserId % tableShards;

            try {
                // 将日增量变更数据，写入表 t_data_align_fans_count_temp_日期_分片序号
                insertMapper.insert2DataAlignUserFollowingCountTempTable(TableConstants.buildTableNameSuffix(date, targetUserIdHashKey), targetUserId);
            } catch (Exception e) {
                log.error("{}", e.getMessage(), e);
            }

            // 数据库写入成功后，再添加布隆过滤器中
            redisTemplate.execute(script, Collections.singletonList(targetUserRedisKey), targetUserId);
        }
        
    }

}
