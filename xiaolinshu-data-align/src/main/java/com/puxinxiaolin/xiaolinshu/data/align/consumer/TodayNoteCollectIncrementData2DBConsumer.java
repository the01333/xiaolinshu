package com.puxinxiaolin.xiaolinshu.data.align.consumer;

import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.data.align.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.data.align.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.data.align.constant.TableConstants;
import com.puxinxiaolin.xiaolinshu.data.align.domain.mapper.InsertMapper;
import com.puxinxiaolin.xiaolinshu.data.align.model.dto.CollectUnCollectNoteMqDTO;
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
 * @Description: 日增量数据落库: 笔记收藏、取消收藏
 * @Author: YCcLin
 * @Date: 2025/7/11 19:33
 */
@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_data_align_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT,
        topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT
)
public class TodayNoteCollectIncrementData2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private InsertMapper insertMapper;

    @Value("${table.shards}")
    private int tableShards;

    @Override
    public void onMessage(String message) {
        log.info("## TodayNoteCollectIncrementData2DBConsumer 消费到了 MQ: {}", message);

        CollectUnCollectNoteMqDTO mqDTO = JsonUtils.parseObject(message, CollectUnCollectNoteMqDTO.class);
        if (Objects.isNull(mqDTO)) {
            return;
        }

        Long noteId = mqDTO.getNoteId();
        Long noteCreatorId = mqDTO.getNoteCreatorId();
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // ------------------------- 笔记的收藏数变更记录 -------------------------
        String noteBloomKey = RedisKeyConstants.buildBloomUserNoteCollectNoteIdListKey(date);

        // 1. 布隆过滤器判断该日增量数据是否已经记录
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_note_collect_check.lua")));

        Long result = redisTemplate.execute(script, Collections.singletonList(noteBloomKey), noteId);

        RedisScript<Long> bloomAddScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);

        // 若布隆过滤器判断不存在（绝对正确）
        if (Objects.equals(result, 0L)) {
            // 若无，才会落库，减轻数据库压力
            long noteIdHashKey = noteId % tableShards;

            try {
                // 将日增量变更数据, 写入表 t_data_align_note_collect_count_temp_日期_分片序号
                insertMapper.insert2DataAlignNoteCollectCountTempTable(TableConstants.buildTableNameSuffix(date, noteIdHashKey), noteId);
            } catch (Exception e) {
                log.error("{}", e.getMessage(), e);
            }
            
            // 3. 数据库写入成功后，再添加布隆过滤器中
            redisTemplate.execute(bloomAddScript, Collections.singletonList(noteBloomKey), noteId);
        }

        // ------------------------- 笔记发布者获得的收藏数变更记录 -------------------------
        String userBloomKey = RedisKeyConstants.buildBloomUserNoteCollectUserIdListKey(date);
        
        // 1. 布隆过滤器判断该日增量数据是否已经记录
        result = redisTemplate.execute(script, Collections.singletonList(userBloomKey), noteCreatorId);
        // 若布隆过滤器判断不存在（绝对正确）
        if (Objects.equals(result, 0L)) {
            // 若无，才会落库，减轻数据库压力
            long userIdHashKey = noteCreatorId % tableShards;
            
            try {
                // 将日增量变更数据, 写入表 t_data_align_user_collect_count_temp_日期_分片序号
                insertMapper.insert2DataAlignUserCollectCountTempTable(TableConstants.buildTableNameSuffix(date, userIdHashKey), noteCreatorId);
            } catch (Exception e) {
                log.error("{}", e.getMessage(), e);
            }
            
            // 3. 数据库写入成功后，再添加布隆过滤器中
            redisTemplate.execute(bloomAddScript, Collections.singletonList(userBloomKey), noteCreatorId);
        }
    }

}
