package com.puxinxiaolin.xiaolinshu.data.align.consumer;

import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.data.align.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.data.align.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.data.align.constant.TableConstants;
import com.puxinxiaolin.xiaolinshu.data.align.domain.mapper.InsertMapper;
import com.puxinxiaolin.xiaolinshu.data.align.model.dto.LikeUnlikeNoteMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

/**
 * @Description: 日增量数据落库: 笔记点赞、取消点赞
 * @Author: YCcLin
 * @Date: 2025/7/11 19:33
 */
@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_data_align_" + MQConstants.TOPIC_COUNT_NOTE_LIKE,
        topic = MQConstants.TOPIC_COUNT_NOTE_LIKE
)
public class TodayNoteLikeIncrementData2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private InsertMapper insertMapper;

    @Value("${table.shards}")
    private int tableShards;

    @Override
    public void onMessage(String message) {
        log.info("## TodayNoteLikeIncrementData2DBConsumer 消费到了 MQ: {}", message);

        LikeUnlikeNoteMqDTO mqDTO = JsonUtils.parseObject(message, LikeUnlikeNoteMqDTO.class);
        if (Objects.isNull(mqDTO)) {
            return;
        }

        Long noteId = mqDTO.getNoteId();
        Long noteCreatorId = mqDTO.getNoteCreatorId();
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // ------------------------- 笔记的点赞数变更记录 -------------------------
        String noteBloomKey = RedisKeyConstants.buildBloomUserNoteLikeNoteIdListKey(date);

        // 1. 布隆过滤器判断该日增量数据是否已经记录
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_note_like_check.lua")));

        Long result = redisTemplate.execute(script, Collections.singletonList(noteBloomKey), noteId);
        
        // 3. 数据库写入成功后，再添加布隆过滤器中
        RedisScript<Long> bloomAddScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
        
        if (Objects.equals(result, 0L)) {
            // 2. 若无，才会落库，减轻数据库压力
            long noteIdHashKey = noteId % tableShards;

            try {
                // 将日增量变更数据落库
                // - t_data_align_note_like_count_temp_日期_分片序号
                insertMapper.insert2DataAlignNoteLikeCountTempTable(TableConstants.buildTableNameSuffix(date, noteIdHashKey), noteId);
            } catch (Exception e) {
                log.error("{}", e.getMessage(), e);
            }

            // 3. 数据库写入成功后，再添加布隆过滤器中
            redisTemplate.execute(bloomAddScript, Collections.singletonList(noteBloomKey), noteId);
        }

        // ------------------------- 笔记发布者获得的点赞数变更记录 -------------------------
        String userBloomKey = RedisKeyConstants.buildBloomUserNoteLikeUserIdListKey(date);

        // 1. 布隆过滤器判断该日增量数据是否已经记录
        result = redisTemplate.execute(script, Collections.singletonList(userBloomKey), noteCreatorId);
        if (Objects.equals(result, 0L)) {
            // 2. 若无，才会落库，减轻数据库压力
            long userIdHashKey = noteCreatorId % tableShards;

            try {
                // 将日增量变更数据落库
                // - t_data_align_user_like_count_temp_日期_分片序号
                insertMapper.insert2DataAlignUserLikeCountTempTable(TableConstants.buildTableNameSuffix(date, userIdHashKey), noteCreatorId);
            } catch (Exception e) {
                log.error("{}", e.getMessage(), e);
            }

            // 3. 数据库写入成功后，再添加布隆过滤器中
            redisTemplate.execute(bloomAddScript, Collections.singletonList(userBloomKey), noteCreatorId);
        }
    }

}
