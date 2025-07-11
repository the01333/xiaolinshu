package com.puxinxiaolin.xiaolinshu.data.align.consumer;

import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.data.align.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.data.align.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.data.align.constant.TableConstants;
import com.puxinxiaolin.xiaolinshu.data.align.domain.mapper.InsertRecordMapper;
import com.puxinxiaolin.xiaolinshu.data.align.model.dto.CollectUnCollectNoteMqDTO;
import com.puxinxiaolin.xiaolinshu.data.align.model.dto.NoteOperateMqDTO;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

/**
 * @Description: 日增量数据落库: 笔记发布、删除
 * @Author: YCcLin
 * @Date: 2025/7/11 19:33
 */
@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_data_align_" + MQConstants.TOPIC_NOTE_OPERATE,
        topic = MQConstants.TOPIC_NOTE_OPERATE
)
public class TodayNotePublishIncrementData2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private InsertRecordMapper insertRecordMapper;

    @Value("${table.shards}")
    private int tableShards;

    @Override
    public void onMessage(String message) {
        log.info("## TodayNotePublishIncrementData2DBConsumer 消费到了 MQ: {}", message);

        NoteOperateMqDTO mqDTO = JsonUtils.parseObject(message, NoteOperateMqDTO.class);
        if (Objects.isNull(mqDTO)) {
            return;
        }
        
        Long noteCreatorId = mqDTO.getCreatorId();
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String redisKey = RedisKeyConstants.buildBloomUserNoteOperateListKey(date);

        // 1. 布隆过滤器判断该日增量数据是否已经记录
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_note_collect_check.lua")));

        Long result = redisTemplate.execute(script, Collections.singletonList(redisKey), noteCreatorId);
        // 若布隆过滤器判断不存在（绝对正确）
        if (Objects.equals(result, 0L)) {
            // 若无，才会落库，减轻数据库压力
            long userIdHashKey = noteCreatorId % tableShards;
            insertRecordMapper.insert2DataAlignUserNotePublishCountTempTable(TableConstants.buildTableNameSuffix(date, userIdHashKey), noteCreatorId);

            // 3. 数据库写入成功后，再添加布隆过滤器中
            RedisScript<Long> bloomAddScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
            redisTemplate.execute(bloomAddScript, Collections.singletonList(redisKey), noteCreatorId);
        }
    }

}
